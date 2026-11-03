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
        this.backend = backend;
        this.enabled = enabled;
        this.configured = List.copyOf(configured);
        this.scheduler = scheduler;
        this.random = random;
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
                    instance = new SimManager(new LiveSimBackend(),
                            YamlConfig.config.server.SIM_PLAYERS_ENABLED,
                            SimBotConfig.parse(YamlConfig.config.server.SIM_BOTS));
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
        int spawned = 0;
        for (SimBotConfig bot : configured) {
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
        return backend.wipeDatabase();
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
