package server.liveops;

import config.EventCalendarConfig;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MaintenanceCountdownTest {
    static class FakeClock extends Clock {
        Instant now = Instant.parse("2026-11-07T10:00:00Z");
        void advance(long secs) { now = now.plusSeconds(secs); }
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    private static final MaintenanceCountdown.Messages MSGS =
            new MaintenanceCountdown.Messages("START {time}", "WARN {time}", "NOW", "CANCEL");

    private static final List<Long> DEFAULT = MaintenanceCountdown.offsets(List.of(10, 5, 1), List.of(30, 10));

    @Test
    void offsetsOnlyIncludeNoticesInsideTheCountdown() {
        assertEquals(List.of(600L, 300L, 60L, 30L, 10L), MaintenanceCountdown.noticeOffsets(3600, DEFAULT));
        assertEquals(List.of(300L, 60L, 30L, 10L), MaintenanceCountdown.noticeOffsets(600, DEFAULT));
        assertEquals(List.of(30L, 10L), MaintenanceCountdown.noticeOffsets(60, DEFAULT));
        assertEquals(List.of(), MaintenanceCountdown.noticeOffsets(5, DEFAULT));
    }

    @Test
    void tenMinuteCountdownNoticesAtTheRightTimes() {
        FakeClock clock = new FakeClock();
        List<String> out = new ArrayList<>();
        List<String> events = new ArrayList<>();
        var cd = new MaintenanceCountdown(clock, Duration.ofMinutes(10), DEFAULT, Duration.ofSeconds(5), MSGS,
                s -> out.add(clock.now.getEpochSecond() % 3600 + ":" + s), () -> events.add("block"), () -> events.add("shutdown"));
        assertEquals(List.of("0:START 10 minutes"), out);
        out.clear();

        for (int t = 1; t <= 600; t++) {
            clock.advance(1);
            cd.tick();
        }
        // clock started at 10:00:00 -> seconds-of-hour = elapsed
        assertEquals(List.of("300:WARN 5 minutes", "540:WARN 1 minute", "570:WARN 30 seconds", "590:WARN 10 seconds", "600:NOW"), out);
        assertTrue(cd.isLoginBlocked());
        assertEquals(List.of("block"), events);

        for (int t = 0; t < 5; t++) {
            clock.advance(1);
            cd.tick();
        }
        assertEquals(List.of("block", "shutdown"), events);
        assertEquals(MaintenanceCountdown.State.DONE, cd.getState());
    }

    @Test
    void oneMinuteCountdownBlocksLoginsAtZeroThenShutsDown() {
        FakeClock clock = new FakeClock();
        List<String> out = new ArrayList<>();
        List<String> events = new ArrayList<>();
        var cd = new MaintenanceCountdown(clock, Duration.ofMinutes(1), DEFAULT, Duration.ofSeconds(5), MSGS,
                out::add, () -> events.add("block"), () -> events.add("shutdown"));
        assertFalse(cd.isLoginBlocked());
        clock.advance(29);
        cd.tick();
        assertFalse(cd.isLoginBlocked());
        clock.advance(1);
        cd.tick();
        assertEquals(List.of("START 1 minute", "WARN 30 seconds"), out);
        clock.advance(29);
        cd.tick();
        assertTrue(events.isEmpty());
        clock.advance(1);
        cd.tick();
        assertTrue(cd.isLoginBlocked());
        assertEquals(List.of("block"), events);
        clock.advance(4);
        cd.tick();
        assertEquals(List.of("block"), events, "grace period not over yet");
        clock.advance(1);
        cd.tick();
        assertEquals(List.of("block", "shutdown"), events);
        clock.advance(100);
        cd.tick();
        assertEquals(2, events.size(), "shutdown fires exactly once");
    }

    @Test
    void lateTickCoalescesSkippedNotices() {
        FakeClock clock = new FakeClock();
        List<String> out = new ArrayList<>();
        var cd = new MaintenanceCountdown(clock, Duration.ofMinutes(10), DEFAULT, Duration.ofSeconds(5), MSGS,
                out::add, () -> {}, () -> {});
        out.clear();
        clock.advance(560); // 40s left: the 5min, 1min notices were both skipped
        cd.tick();
        assertEquals(List.of("WARN 40 seconds"), out);
    }

    @Test
    void cancelOnlyWorksBeforeLoginBlock() {
        FakeClock clock = new FakeClock();
        List<String> out = new ArrayList<>();
        var cd = new MaintenanceCountdown(clock, Duration.ofMinutes(2), DEFAULT, Duration.ofSeconds(5), MSGS,
                out::add, () -> fail("must not block"), () -> fail("must not shut down"));
        assertTrue(cd.cancel());
        assertEquals("CANCEL", out.get(out.size() - 1));
        clock.advance(500);
        cd.tick();
        assertFalse(cd.isLoginBlocked());

        var cd2 = new MaintenanceCountdown(clock, Duration.ofSeconds(1), DEFAULT, Duration.ofSeconds(5), MSGS, s -> {}, () -> {}, () -> {});
        clock.advance(1);
        cd2.tick();
        assertFalse(cd2.cancel());
    }

    // ---- event window notices ----

    private static final ZoneId Z = ZoneId.of("UTC");

    private static ZonedDateTime at(int h, int m, int s) {
        return ZonedDateTime.of(2026, 11, 7, h, m, s, 0, Z); // Saturday
    }

    @Test
    void eventNoticesFireOnceAtT10_T5_T1_startAndEnd() {
        Map<String, Object> e = new HashMap<>();
        e.put("name", "Weekend 2x");
        e.put("start", "18:00");
        e.put("duration_minutes", 60);
        e.put("exp_mult", 2);
        e.put("announce", "Weekend 2x is LIVE");
        List<EventWindow> windows = EventCalendar.parseWindows(List.of(e));
        List<String> out = new ArrayList<>();
        EventCalendarConfig cfg = new EventCalendarConfig();
        EventNotices n = new EventNotices(windows, cfg, (w, msg) -> out.add(w + ":" + msg));

        // tick every 15s from 17:00 to 19:05, each tick twice to prove dedupe
        ZonedDateTime t = at(17, 0, 0);
        List<String> stamps = new ArrayList<>();
        while (t.isBefore(at(19, 5, 0))) {
            int before = out.size();
            n.tick(t);
            n.tick(t);
            if (out.size() > before) {
                stamps.add(t.toLocalTime() + " " + out.get(out.size() - 1));
            }
            t = t.plusSeconds(15);
        }
        assertEquals(List.of(
                "17:50 -1:[Event] Weekend 2x starts in 10 minute(s)!",
                "17:55 -1:[Event] Weekend 2x starts in 5 minute(s)!",
                "17:59 -1:[Event] Weekend 2x starts in 1 minute(s)!",
                "18:00 -1:Weekend 2x is LIVE",
                "19:00 -1:[Event] Weekend 2x has ended. Thanks for playing!"), stamps);
    }

    @Test
    void restartMidEventDoesNotReplayOldNotices() {
        Map<String, Object> e = new HashMap<>();
        e.put("name", "X");
        e.put("start", "18:00");
        e.put("duration_minutes", 60);
        e.put("exp_mult", 2);
        List<String> out = new ArrayList<>();
        EventNotices n = new EventNotices(EventCalendar.parseWindows(List.of(e)), new EventCalendarConfig(), (w, m) -> out.add(m));
        n.tick(at(18, 30, 0));
        assertTrue(out.isEmpty());
    }
}
