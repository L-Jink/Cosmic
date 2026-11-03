package server.liveops;

import config.EventCalendarConfig;
import config.YamlConfig;
import net.server.world.World;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import server.TimerManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Real wiring path: config.local.yaml override merged over config.yaml -> EventCalendar.start -> the shared
 * TimerManager -> World setters. Only the clock is faked (and World is a mock, so no database is needed).
 */
class EventCalendarWiringTest {
    @Test
    void localOverrideConfigDrivesRealTimerAndRealWorldSink() throws Exception {
        Path local = Files.createTempFile("config.local", ".yaml");
        Files.writeString(local, "event_calendar:\n  ENABLED: true\n  events:\n    - name: Test 2x EXP\n"
                + "      world: all\n      start: \"21:45\"\n      duration_minutes: 2\n      exp_mult: 2\n");
        var load = YamlConfig.class.getDeclaredMethod("load", Path.class, Path.class, String.class);
        load.setAccessible(true);
        EventCalendarConfig cfg = ((YamlConfig) load.invoke(null, Path.of("config.yaml"), local, null)).event_calendar;
        cfg.CHECK_INTERVAL_SECONDS = 1;

        World world = Mockito.mock(World.class);
        when(world.getId()).thenReturn(0);
        when(world.getExpRate()).thenReturn(10);
        when(world.getDropRate()).thenReturn(10);
        when(world.getMesoRate()).thenReturn(10);

        ZoneId zone = EventCalendar.hostZone(); // blank TIMEZONE = the PC's own zone, whatever that is
        ZonedDateTime[] fake = {ZonedDateTime.of(2026, 11, 3, 21, 44, 59, 0, zone)};
        TimerManager.getInstance().start();
        EventCalendar cal = EventCalendar.start(cfg, () -> List.of(world), z -> fake[0].withZoneSameInstant(z));
        try {
            Thread.sleep(2500);
            verify(world, never()).setExpRate(anyInt());
            fake[0] = ZonedDateTime.of(2026, 11, 3, 21, 45, 0, 0, zone);
            verify(world, timeout(5000)).setExpRate(20);
            fake[0] = ZonedDateTime.of(2026, 11, 3, 21, 47, 0, 0, zone);
            verify(world, timeout(5000)).setExpRate(10);
        } finally {
            TimerManager.getInstance().stop();
        }
    }
}
