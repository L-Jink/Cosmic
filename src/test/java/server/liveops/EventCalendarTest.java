package server.liveops;

import config.EventCalendarConfig;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventCalendarTest {
    private static final ZoneId Z = ZoneId.of("UTC");

    /** 2026-11-07 is a Saturday. */
    private static ZonedDateTime at(int day, int h, int m) {
        return ZonedDateTime.of(2026, 11, day, h, m, 0, 0, Z);
    }

    static class FakeWorld implements RateSink {
        int exp, drop, meso;
        FakeWorld(int e, int d, int m) { exp = e; drop = d; meso = m; }
        public int getExpRate() { return exp; }
        public void setExpRate(int r) { exp = r; }
        public int getDropRate() { return drop; }
        public void setDropRate(int r) { drop = r; }
        public int getMesoRate() { return meso; }
        public void setMesoRate(int r) { meso = r; }
    }

    private static Map<String, Object> entry(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static EventCalendar cal(Map<Integer, RateSink> worlds, Map<String, Object>... entries) {
        return new EventCalendar(EventCalendar.parseWindows(List.of(entries)), () -> worlds);
    }

    @Test
    void windowStartsAndEndsAndRestoresBaseRates() {
        FakeWorld w = new FakeWorld(10, 10, 10);
        var c = cal(Map.of(0, w), entry("name", "2x", "world", 0, "start", "18:00", "duration_minutes", 120, "exp_mult", 2, "drop_mult", 2));

        c.tick(at(7, 17, 59));
        assertEquals(10, w.exp);
        c.tick(at(7, 18, 0));
        assertEquals(20, w.exp);
        assertEquals(20, w.drop);
        assertEquals(10, w.meso, "meso_mult not set -> untouched");
        c.tick(at(7, 19, 59));
        assertEquals(20, w.exp);
        c.tick(at(7, 20, 0));
        assertEquals(10, w.exp);
        assertEquals(10, w.drop);
    }

    @Test
    void otherWorldsAreUntouchedAndAllWorks() {
        FakeWorld a = new FakeWorld(10, 10, 10), b = new FakeWorld(5, 5, 5);
        var c = cal(Map.of(0, a, 1, b), entry("name", "w1", "world", 1, "start", "9:00", "duration_minutes", 60, "meso_mult", 3));
        c.tick(at(7, 9, 30));
        assertEquals(10, a.meso);
        assertEquals(15, b.meso);
    }

    @Test
    void overlappingWindowsDoNotStackHighestWins() {
        FakeWorld w = new FakeWorld(10, 10, 10);
        var c = cal(Map.of(0, w),
                entry("name", "a", "world", "all", "start", "18:00", "duration_minutes", 120, "exp_mult", 2),
                entry("name", "b", "world", 0, "start", "19:00", "duration_minutes", 120, "exp_mult", 3, "drop_mult", 2));
        c.tick(at(7, 18, 30));
        assertEquals(20, w.exp);
        c.tick(at(7, 19, 30));
        assertEquals(30, w.exp, "highest (3x), not 2x*3x");
        assertEquals(20, w.drop);
        c.tick(at(7, 20, 30));
        assertEquals(30, w.exp);
        c.tick(at(7, 21, 0));
        assertEquals(10, w.exp);
        assertEquals(10, w.drop);
    }

    @Test
    void daysFilterAndMidnightCrossing() {
        FakeWorld w = new FakeWorld(1, 1, 1);
        var c = cal(Map.of(0, w), entry("name", "sat night", "start", "23:00", "duration_minutes", 120,
                "exp_mult", 2, "days", List.of("SAT")));
        c.tick(at(6, 23, 30)); // Friday
        assertEquals(1, w.exp);
        c.tick(at(7, 23, 30)); // Saturday
        assertEquals(2, w.exp);
        c.tick(at(8, 0, 30)); // Sunday, still inside Saturday's window
        assertEquals(2, w.exp);
        c.tick(at(8, 1, 0));
        assertEquals(1, w.exp);
    }

    @Test
    void disabledOrMissingConfigYieldsNoCalendar() {
        EventCalendarConfig cfg = new EventCalendarConfig();
        cfg.events = List.of(entry("name", "x", "start", "10:00", "duration_minutes", 5, "exp_mult", 2));
        assertNull(EventCalendar.fromConfig(cfg, Map::of), "ENABLED defaults to false");
        assertNull(EventCalendar.fromConfig(null, Map::of));
        cfg.ENABLED = true;
        assertNotNull(EventCalendar.fromConfig(cfg, Map::of));
    }

    @Test
    void badEntriesAreIgnoredWithWarning() {
        List<String> warnings = new ArrayList<>();
        {
            List<Map<String, Object>> entries = new ArrayList<>();
            entries.add(entry("name", "no start", "duration_minutes", 5, "exp_mult", 2));
            entries.add(entry("name", "bad dur", "start", "10:00", "duration_minutes", "abc", "exp_mult", 2));
            entries.add(entry("name", "zero dur", "start", "10:00", "duration_minutes", 0, "exp_mult", 2));
            entries.add(entry("name", "no mult", "start", "10:00", "duration_minutes", 5));
            entries.add(entry("name", "bad day", "start", "10:00", "duration_minutes", 5, "exp_mult", 2, "days", "FUNDAY"));
            entries.add(entry("name", "bad mult", "start", "10:00", "duration_minutes", 5, "exp_mult", 0));
            entries.add(entry("name", "good", "start", "10:00", "duration_minutes", 5, "exp_mult", 2));
            entries.add(null);
            List<EventWindow> out = EventCalendar.parseWindows(entries, warnings::add);
            assertEquals(1, out.size());
            assertEquals("good", out.get(0).name());
            assertEquals(7, warnings.size());
            assertTrue(warnings.get(0).contains("no start"));
        }
    }

    @Test
    void restoreAllPutsBaseRatesBack() {
        FakeWorld w = new FakeWorld(10, 10, 10);
        var c = cal(Map.of(0, w), entry("name", "2x", "start", "18:00", "duration_minutes", 60, "exp_mult", 2));
        c.tick(at(7, 18, 5));
        assertEquals(20, w.exp);
        c.restoreAll();
        assertEquals(10, w.exp);
    }

    @Test
    void blankTimezoneIsThePcZoneEvenAfterJvmDefaultIsForcedToGmt() {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Singapore"));
            EventCalendar.captureHostZone(); // no-op if an earlier test already captured; both cases are checked below
            ZoneId pc = EventCalendar.hostZone();
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("GMT")); // what Server.init does
            assertEquals(pc, EventCalendar.parseZone(""));
            assertEquals(pc, EventCalendar.parseZone(null));
            assertEquals(pc, EventCalendar.parseZone("   "));
            assertEquals(pc, EventCalendar.parseZone("Not/AZone"));
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    @Test
    void explicitTimezoneWins() {
        assertEquals(ZoneId.of("Asia/Singapore"), EventCalendar.parseZone("Asia/Singapore"));
        assertEquals(ZoneId.of("America/New_York"), EventCalendar.parseZone(" America/New_York "));
    }
}
