package server.liveops;

import config.EventCalendarConfig;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * World notices for calendar windows: "starts in N minutes" (T-10/T-5/T-1 by default), start (the window's
 * {@code announce} text, else the default) and end. Tick-driven like {@link EventCalendar}; each notice fires once.
 * A notice is only sent if the tick lands within {@code catchup} of its moment, so a restart mid-window does not
 * replay stale announcements.
 */
public class EventNotices {
    private final List<EventWindow> windows;
    private final List<Integer> noticeMinutes;
    private final String msgSoon, msgStart, msgEnd;
    private final BiConsumer<Integer, String> notifier;
    private final Duration catchup;
    private final Set<String> fired = new HashSet<>();

    public EventNotices(List<EventWindow> windows, EventCalendarConfig cfg, BiConsumer<Integer, String> notifier) {
        this.windows = windows;
        this.noticeMinutes = cfg.NOTICE_MINUTES == null ? List.of() : cfg.NOTICE_MINUTES;
        this.msgSoon = cfg.MSG_EVENT_SOON;
        this.msgStart = cfg.MSG_EVENT_START;
        this.msgEnd = cfg.MSG_EVENT_END;
        this.notifier = notifier;
        this.catchup = Duration.ofSeconds(Math.max(60, 2L * cfg.CHECK_INTERVAL_SECONDS));
    }

    public void tick(ZonedDateTime now) {
        fired.removeIf(k -> Long.parseLong(k.substring(k.lastIndexOf('|') + 1)) < now.minusDays(2).toEpochSecond());
        for (int i = 0; i < windows.size(); i++) {
            EventWindow w = windows.get(i);
            int daysBack = w.durationMinutes() / (24 * 60) + 1;
            for (int k = -1; k <= daysBack; k++) {
                LocalDate date = now.toLocalDate().minusDays(k);
                if (!w.days().contains(date.getDayOfWeek())) {
                    continue;
                }
                ZonedDateTime begin = date.atTime(w.start()).atZone(now.getZone());
                for (int m : noticeMinutes) {
                    if (m > 0) {
                        fire(i, w, "soon" + m, begin.minusMinutes(m), now,
                                msgSoon.replace("{name}", w.name()).replace("{minutes}", String.valueOf(m)));
                    }
                }
                String start = w.announce() != null && !w.announce().isBlank() ? w.announce() : msgStart.replace("{name}", w.name());
                fire(i, w, "start", begin, now, start);
                fire(i, w, "end", begin.plusMinutes(w.durationMinutes()), now, msgEnd.replace("{name}", w.name()));
            }
        }
    }

    private void fire(int idx, EventWindow w, String kind, ZonedDateTime at, ZonedDateTime now, String text) {
        if (now.isBefore(at) || !now.isBefore(at.plus(catchup))) {
            return;
        }
        if (fired.add(idx + "|" + kind + "|" + at.toEpochSecond())) {
            notifier.accept(w.world(), text);
        }
    }
}
