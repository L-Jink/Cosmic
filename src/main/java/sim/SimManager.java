package sim;

import config.YamlConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public SimManager(SimBackend backend, boolean enabled, List<SimBotConfig> configured) {
        this.backend = backend;
        this.enabled = enabled;
        this.configured = List.copyOf(configured);
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
