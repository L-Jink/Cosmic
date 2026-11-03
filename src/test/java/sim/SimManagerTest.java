package sim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimManagerTest {
    /** Fake server/DB: records what happened. */
    static class FakeBackend implements SimBackend {
        final List<String> entered = new ArrayList<>();
        final List<String> left = new ArrayList<>();
        int wipes;
        int nextId = 100;
        String failFor;

        @Override
        public SimHandle enter(SimBotConfig bot) throws Exception {
            if (bot.name().equals(failFor)) {
                throw new IllegalStateException("boom");
            }
            entered.add(bot.name());
            int id = nextId++;
            return new SimHandle() {
                public int characterId() { return id; }
                public int mapId() { return bot.homeMap(); }
                public void leave() { left.add(bot.name()); }
            };
        }

        @Override
        public int wipeDatabase() {
            wipes++;
            return entered.size();
        }
    }

    private static Map<String, Object> bot(String name, int job, int level, int map, int ch) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("job", job);
        m.put("level", level);
        m.put("homeMap", map);
        m.put("channel", ch);
        return m;
    }

    private static List<SimBotConfig> twoBots() {
        return SimBotConfig.parse(List.of(bot("Mapleaf", 110, 35, 100000000, 1), bot("Moonpie", 310, 40, 100000000, 1)));
    }

    @Test
    void parsesValidEntriesAndSkipsInvalidOnes() {
        Map<String, Object> badName = bot("ab", 0, 1, 100000000, 1);
        Map<String, Object> noMap = bot("Goodname", 0, 1, 0, 1);
        Map<String, Object> badLevel = bot("Levelbad", 0, 0, 100000000, 1);
        Map<String, Object> dup = bot("mapleaf", 0, 1, 100000000, 1);
        Map<String, Object> strings = new HashMap<>();   // YAML reader hands back strings
        strings.put("name", "Stringy");
        strings.put("job", "210");
        strings.put("level", "20");
        strings.put("homeMap", "100000000");
        strings.put("channel", "2");
        strings.put("gender", "1");

        List<SimBotConfig> parsed = SimBotConfig.parse(
                List.of(bot("Mapleaf", 110, 35, 100000000, 1), badName, noMap, badLevel, dup, strings));

        assertEquals(2, parsed.size());
        assertEquals(new SimBotConfig("Mapleaf", 110, 35, 100000000, 1, 0), parsed.get(0));
        assertEquals(new SimBotConfig("Stringy", 210, 20, 100000000, 2, 1), parsed.get(1));
        assertTrue(SimBotConfig.parse(null).isEmpty());
    }

    @Test
    void disabledFlagDoesNothing() {
        FakeBackend backend = new FakeBackend();
        SimManager m = new SimManager(backend, false, twoBots());

        assertEquals(0, m.spawnConfigured());
        assertEquals(SimManager.SpawnResult.DISABLED, m.spawn("Mapleaf"));
        assertTrue(backend.entered.isEmpty());
        assertEquals(0, m.onlineCount());
    }

    @Test
    void spawnsAllConfiguredBotsOnce() {
        FakeBackend backend = new FakeBackend();
        SimManager m = new SimManager(backend, true, twoBots());

        assertEquals(2, m.spawnConfigured());
        assertEquals(List.of("Mapleaf", "Moonpie"), backend.entered);
        assertEquals(2, m.onlineCount());
        assertEquals(SimManager.SpawnResult.ALREADY_ONLINE, m.spawn("mapleaf"));
        assertEquals(2, backend.entered.size());
    }

    @Test
    void spawnAndRemoveSingleBot() {
        FakeBackend backend = new FakeBackend();
        SimManager m = new SimManager(backend, true, twoBots());

        assertEquals(SimManager.SpawnResult.UNKNOWN, m.spawn("Nobody"));
        assertEquals(SimManager.SpawnResult.SPAWNED, m.spawn("Moonpie"));
        assertTrue(m.isOnline("moonpie"));
        assertFalse(m.isOnline("Mapleaf"));

        assertFalse(m.remove("Mapleaf"));
        assertTrue(m.remove("MOONPIE"));
        assertEquals(List.of("Moonpie"), backend.left);
        assertEquals(0, m.onlineCount());
        // can come back
        assertEquals(SimManager.SpawnResult.SPAWNED, m.spawn("Moonpie"));
    }

    @Test
    void failedSpawnDoesNotStopTheOthers() {
        FakeBackend backend = new FakeBackend();
        backend.failFor = "Mapleaf";
        SimManager m = new SimManager(backend, true, twoBots());

        assertEquals(1, m.spawnConfigured());
        assertFalse(m.isOnline("Mapleaf"));
        assertTrue(m.isOnline("Moonpie"));
    }

    @Test
    void wipeRemovesOnlineBotsAndClearsDatabase() {
        FakeBackend backend = new FakeBackend();
        SimManager m = new SimManager(backend, true, twoBots());
        m.spawnConfigured();

        assertEquals(2, m.wipe());
        assertEquals(2, backend.left.size());
        assertEquals(1, backend.wipes);
        assertEquals(0, m.onlineCount());
        assertTrue(m.describe().get(0).contains("[offline]"));
    }
}
