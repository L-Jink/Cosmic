package sim;

import config.YamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.TimerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Registry of simulated players ("residents"). Everything is a no-op unless SIM_PLAYERS_ENABLED is true.
 * No per-bot threads: bots are plain server-side characters with a socketless client.
 */
public class SimManager {
    private static final Logger log = LoggerFactory.getLogger(SimManager.class);
    private static volatile SimManager instance;

    private final SimBackend backend;
    private final boolean enabled;
    private final List<SimBotConfig> configured;
    private final Map<String, SimBackend.SimHandle> online = new TreeMap<>();
    private final Map<String, SimState> states = new TreeMap<>();
    private final SimScheduler scheduler;
    private final Random random;

    /** What a bot is doing right now. More states (following, leaving, ...) arrive in later cards. */
    public enum SimState { IDLE, RESPONDING }

    /** Runs a task once after a delay (production: TimerManager; tests: run immediately). */
    @FunctionalInterface
    public interface SimScheduler {
        void schedule(Runnable task, long delayMs);
    }

    private static final String[] GREETINGS = {"hi", "hello", "hey", "hi all", "hey guys", "yo"};
    /** Human-like pause before accepting an invite, and before the greeting. */
    static final int ACCEPT_DELAY_MIN_MS = 1500;
    static final int ACCEPT_DELAY_SPREAD_MS = 2500;
    static final int GREET_DELAY_MIN_MS = 1200;
    static final int GREET_DELAY_SPREAD_MS = 1800;

    public SimManager(SimBackend backend, boolean enabled, List<SimBotConfig> configured) {
        this(backend, enabled, configured, (task, delay) -> TimerManager.getInstance().schedule(task, delay), new Random());
    }

    public SimManager(SimBackend backend, boolean enabled, List<SimBotConfig> configured, SimScheduler scheduler, Random random) {
        this(backend, enabled, configured, scheduler, random, null, null);
    }

    /**
     * @param population when not null, only the bots the hourly curve wants are online
     * @param phrases    when not null, idle bots walk, sit and chat (the routine); null = bots just stand
     */
    public SimManager(SimBackend backend, boolean enabled, List<SimBotConfig> configured, SimScheduler scheduler, Random random,
                      SimPopulation population, SimPhrases phrases) {
        this.backend = backend;
        this.enabled = enabled;
        this.configured = List.copyOf(configured);
        this.scheduler = scheduler;
        this.random = random;
        this.population = population;
        this.phrases = phrases;
    }

    // ---------------------------------------------------------------- idle routine + population

    private final SimPopulation population;
    private final SimPhrases phrases;
    private final Map<String, Routine> routines = new TreeMap<>();
    /** Bots a GM removed by hand: the population curve leaves them offline until a GM spawns them again. */
    private final java.util.Set<String> manuallyOff = new java.util.TreeSet<>();
    private java.util.concurrent.ScheduledFuture<?> routineTask;
    private long nextPopulationAt;

    static final int CHAIR_ITEM = 3010000; // The Relaxer
    static final long TICK_MS = 1000;
    static final long POPULATION_PERIOD_MIN_MS = 20_000;
    static final long POPULATION_PERIOD_SPREAD_MS = 20_000;
    static final long CHAT_GAP_MIN_MS = 90_000;
    static final long CHAT_GAP_SPREAD_MS = 210_000;

    /** Per-bot idle-routine bookkeeping. */
    private static final class Routine {
        long nextActionAt;
        long nextChatAt;
        String lastPhrase;
    }

    /** Starts the one shared task that drives every bot's routine and the population curve. */
    synchronized void startRoutine() {
        if (!enabled || phrases == null || routineTask != null) {
            return;
        }
        routineTask = TimerManager.getInstance().register(() -> {
            try {
                tick(System.currentTimeMillis(), java.time.LocalTime.now().getHour());
            } catch (Throwable t) { // an exception would silently cancel the periodic task
                log.warn("Sim routine tick failed", t);
            }
        }, TICK_MS, TICK_MS);
    }

    synchronized void stopRoutine() {
        if (routineTask != null) {
            routineTask.cancel(false);
            routineTask = null;
        }
    }

    /** One step of the routine for every online bot. {@code hour} is the local hour of day (0-23). */
    public void tick(long nowMs, int hour) {
        if (!enabled || phrases == null) {
            return;
        }
        List<Map.Entry<String, SimBackend.SimHandle>> snapshot;
        synchronized (this) {
            populationStep(nowMs, hour);
            snapshot = new ArrayList<>(online.entrySet());
        }
        for (Map.Entry<String, SimBackend.SimHandle> e : snapshot) {
            try {
                routineStep(e.getKey(), e.getValue(), nowMs);
            } catch (RuntimeException ex) {
                log.warn("Sim routine failed for {}: {}", e.getKey(), ex.toString());
            }
        }
    }

    /** Brings one bot in or out (at most) towards what the curve wants, so crowds build up and thin out gradually. */
    private void populationStep(long now, int hour) {
        if (population == null || now < nextPopulationAt) {
            return;
        }
        nextPopulationAt = now + POPULATION_PERIOD_MIN_MS + random.nextInt((int) POPULATION_PERIOD_SPREAD_MS);
        java.util.Set<String> wanted = new java.util.TreeSet<>();
        for (SimBotConfig b : population.desired(configured, hour)) {
            wanted.add(key(b.name()));
        }
        for (String k : wanted) {
            if (!online.containsKey(k) && !manuallyOff.contains(k)) {
                spawnInternal(findConfigured(k));
                return;
            }
        }
        for (Map.Entry<String, SimBackend.SimHandle> e : online.entrySet()) {
            SimBackend.SimHandle h = e.getValue();
            if (!wanted.contains(e.getKey()) && !h.isInParty() && states.get(e.getKey()) == null) {
                removeInternal(e.getKey());
                return;
            }
        }
    }

    private void routineStep(String k, SimBackend.SimHandle h, long now) {
        Routine r;
        synchronized (this) {
            if (online.get(k) != h) {
                return;
            }
            r = routines.get(k);
            if (r == null) {
                r = new Routine();
                r.nextActionAt = now + 2000 + random.nextInt(6000);
                r.nextChatAt = now + 30_000 + random.nextInt(90_000);
                routines.put(k, r);
            }
            if (states.get(k) != null || h.isInParty()) {
                return; // busy answering a party invite / partied: the routine stays out of the way
            }
        }
        if (now >= r.nextChatAt) {
            String line = phrases.pick(random, r.lastPhrase);
            r.lastPhrase = line;
            r.nextChatAt = now + CHAT_GAP_MIN_MS + random.nextInt((int) CHAT_GAP_SPREAD_MS);
            h.say(line);
        }
        if (now < r.nextActionAt) {
            return;
        }
        if (h.isSitting()) {
            h.stand();
            r.nextActionAt = now + 2000 + random.nextInt(4000);
            return;
        }
        int roll = random.nextInt(100);
        if (roll < 55) {
            java.awt.Point pos = h.position();
            java.awt.Rectangle bounds = h.walkBounds();
            SimRoutine.Move move = pos == null || bounds == null ? null
                    : SimRoutine.planWalk(pos, bounds, h::groundBelow, random);
            if (move != null) {
                h.walkTo(move.to(), move.right(), move.durationMs());
                r.nextActionAt = now + move.durationMs() + 1500 + random.nextInt(6000);
                return;
            }
        } else if (roll < 65 && h.sit(CHAIR_ITEM)) {
            r.nextActionAt = now + 30_000 + random.nextInt(90_000);
            return;
        }
        r.nextActionAt = now + 2000 + random.nextInt(8000); // just stand around for a bit
    }

    private SimBotConfig findConfigured(String k) {
        return configured.stream().filter(b -> key(b.name()).equals(k)).findFirst().orElseThrow();
    }

    public synchronized SimState stateOf(String name) {
        return states.getOrDefault(key(name), SimState.IDLE);
    }

    private synchronized SimBackend.SimHandle handleById(int characterId) {
        for (SimBackend.SimHandle h : online.values()) {
            if (h.characterId() == characterId) {
                return h;
            }
        }
        return null;
    }

    /**
     * A real player invited this character to a party. Bots answer after a short random delay: accept (then say hi),
     * or decline straight away when already in a party / busy answering another invite / sims are off.
     */
    public void onPartyInvite(int botCharacterId, int partyId) {
        SimBackend.SimHandle h = enabled ? handleById(botCharacterId) : null;
        if (h == null) {
            backend.declineStrayPartyInvite(botCharacterId, partyId);
            return;
        }
        String k = key(h.name());
        synchronized (this) {
            if (h.isInParty() || states.get(k) == SimState.RESPONDING) {
                h.declinePartyInvite(partyId);
                return;
            }
            states.put(k, SimState.RESPONDING);
        }
        scheduler.schedule(() -> finishPartyInvite(h, k, partyId), ACCEPT_DELAY_MIN_MS + random.nextInt(ACCEPT_DELAY_SPREAD_MS));
    }

    private void finishPartyInvite(SimBackend.SimHandle h, String k, int partyId) {
        synchronized (this) {
            states.remove(k);
            if (online.get(k) != h) {
                return; // removed while thinking
            }
        }
        if (!h.acceptPartyInvite(partyId)) {
            return;
        }
        scheduler.schedule(() -> {
            synchronized (this) {
                if (online.get(k) != h) {
                    return;
                }
            }
            h.partySay(GREETINGS[random.nextInt(GREETINGS.length)]);
        }, GREET_DELAY_MIN_MS + random.nextInt(GREET_DELAY_SPREAD_MS));
    }

    /** GM/testing: the bot invites a real player. @return null if the bot is not online. */
    public SimParty.InviteOutcome invite(String botName, String playerName) {
        SimBackend.SimHandle h = onlineHandle(botName);
        return h == null ? null : h.invite(playerName);
    }

    /** GM/testing: the bot says something in party chat. @return false if offline or not in a party. */
    public boolean partySay(String botName, String text) {
        SimBackend.SimHandle h = onlineHandle(botName);
        return h != null && h.partySay(text);
    }

    /** GM/testing: the bot leaves its party. @return false if offline or not in a party. */
    public boolean leaveParty(String botName) {
        SimBackend.SimHandle h = onlineHandle(botName);
        if (h == null || !h.isInParty()) {
            return false;
        }
        h.leaveParty();
        return true;
    }

    private synchronized SimBackend.SimHandle onlineHandle(String name) {
        return online.get(key(name));
    }

    public static SimManager getInstance() {
        SimManager m = instance;
        if (m == null) {
            synchronized (SimManager.class) {
                if (instance == null) {
                    var cfg = YamlConfig.config.server;
                    boolean routine = cfg.SIM_ROUTINE_ENABLED;
                    instance = new SimManager(new LiveSimBackend(), cfg.SIM_PLAYERS_ENABLED,
                            SimBotConfig.parse(cfg.SIM_BOTS),
                            (task, delay) -> TimerManager.getInstance().schedule(task, delay), new Random(),
                            routine ? new SimPopulation(SimPopulation.parseCurve(cfg.SIM_POPULATION_CURVE),
                                    cfg.SIM_MAX_BOTS_PER_MAP, cfg.SIM_MAX_BOTS_PER_CHANNEL) : null,
                            routine && cfg.SIM_PLAYERS_ENABLED ? SimPhrases.load(cfg.SIM_PHRASES_FILE) : null);
                }
                m = instance;
            }
        }
        return m;
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<SimBotConfig> getConfigured() {
        return configured;
    }

    /** Called once when the server is online. Does nothing when the flag is off. */
    public synchronized int spawnConfigured() {
        if (!enabled) {
            return 0;
        }
        int spawned = spawnInitial(java.time.LocalTime.now().getHour());
        startRoutine();
        return spawned;
    }

    /** Spawns the bots the curve wants at {@code hour} (all configured bots when there is no curve). */
    synchronized int spawnInitial(int hour) {
        if (!enabled) {
            return 0;
        }
        int spawned = 0;
        List<SimBotConfig> initial = population == null ? configured : population.desired(configured, hour);
        for (SimBotConfig bot : initial) {
            if (spawn(bot.name()) == SpawnResult.SPAWNED) {
                spawned++;
            }
        }
        log.info("Sim players: {} of {} configured bot(s) spawned", spawned, configured.size());
        return spawned;
    }

    public enum SpawnResult { SPAWNED, DISABLED, UNKNOWN, ALREADY_ONLINE, FAILED }

    public synchronized SpawnResult spawn(String name) {
        if (!enabled) {
            return SpawnResult.DISABLED;
        }
        SimBotConfig bot = configured.stream().filter(b -> b.name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (bot == null) {
            return SpawnResult.UNKNOWN;
        }
        manuallyOff.remove(key(bot.name()));
        return spawnInternal(bot);
    }

    private SpawnResult spawnInternal(SimBotConfig bot) {
        if (online.containsKey(key(bot.name()))) {
            return SpawnResult.ALREADY_ONLINE;
        }
        try {
            online.put(key(bot.name()), backend.enter(bot));
            return SpawnResult.SPAWNED;
        } catch (Exception e) {
            log.warn("Could not spawn sim bot {}: {}", bot.name(), e.toString());
            return SpawnResult.FAILED;
        }
    }

    /** @return true if the bot was online and has been removed from the world (its DB row stays). */
    public synchronized boolean remove(String name) {
        manuallyOff.add(key(name));
        return removeInternal(key(name));
    }

    private boolean removeInternal(String name) {
        routines.remove(key(name));
        SimBackend.SimHandle h = online.remove(key(name));
        states.remove(key(name));
        if (h == null) {
            return false;
        }
        h.leave();
        return true;
    }

    /** Removes every online bot and deletes all is_sim rows from the DB. @return rows deleted. */
    public synchronized int wipe() {
        for (String name : new ArrayList<>(online.keySet())) {
            remove(name);
        }
        int removed = backend.wipeDatabase();
        manuallyOff.clear();
        return removed;
    }

    public synchronized boolean isOnline(String name) {
        return online.containsKey(key(name));
    }

    public synchronized int onlineCount() {
        return online.size();
    }

    /** One line per configured bot for the GM list command. */
    public synchronized List<String> describe() {
        List<String> lines = new ArrayList<>();
        for (SimBotConfig b : configured) {
            SimBackend.SimHandle h = online.get(key(b.name()));
            lines.add(b.name() + " lv" + b.level() + " job " + b.job() + " home " + b.homeMap() + " ch" + b.channel()
                    + (h != null ? " [online, map " + h.mapId() + "]" : " [offline]"));
        }
        return lines;
    }
}
