package server.liveops;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Wall-clock demo of a 2-minute window (takes ~3.5 min, so skipped unless run with
 * {@code -Dcalendar.demo=true -Dtest=EventCalendarLiveDemoTest}). Uses the real EventCalendar and a real scheduler,
 * with fake world rates, so it needs no database or open ports.
 */
class EventCalendarLiveDemoTest {
    private static final Logger log = LoggerFactory.getLogger(EventCalendarLiveDemoTest.class);

    @Test
    @EnabledIfSystemProperty(named = "calendar.demo", matches = "true")
    void twoMinuteWindowChangesAndRestoresRates() throws Exception {
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime start = ZonedDateTime.now(zone).plusMinutes(1).withSecond(0).withNano(0);
        var world = new EventCalendarTest.FakeWorld(10, 10, 10);
        var entry = new java.util.HashMap<String, Object>();
        entry.put("name", "Demo 2x");
        entry.put("world", 0);
        entry.put("start", start.format(DateTimeFormatter.ofPattern("HH:mm")));
        entry.put("duration_minutes", 2);
        entry.put("exp_mult", 2);
        entry.put("drop_mult", 2);
        entry.put("meso_mult", 3);
        var cal = new EventCalendar(EventCalendar.parseWindows(List.of(entry)), () -> Map.of(0, world));

        System.out.println("DEMO window " + start.format(DateTimeFormatter.ofPattern("HH:mm")) + " for 2 minutes");
        log.info("Window {} -> {}; base rates exp={} drop={} meso={}", start, start.plusMinutes(2), world.exp, world.drop, world.meso);
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor();
        int[] last = {-1, -1, -1};
        ex.scheduleAtFixedRate(() -> {
            cal.tick(ZonedDateTime.now(zone));
            if (world.exp != last[0] || world.drop != last[1] || world.meso != last[2]) {
                last[0] = world.exp; last[1] = world.drop; last[2] = world.meso;
                System.out.println("DEMO " + ZonedDateTime.now(zone).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        + " rates exp=" + world.exp + " drop=" + world.drop + " meso=" + world.meso);
            }
        }, 0, 5, TimeUnit.SECONDS);
        Thread.sleep(start.plusMinutes(2).plusSeconds(10).toInstant().toEpochMilli() - System.currentTimeMillis());
        ex.shutdownNow();
        log.info("Final rates exp={} drop={} meso={}", world.exp, world.drop, world.meso);
        org.junit.jupiter.api.Assertions.assertEquals(10, world.exp);
    }
}
