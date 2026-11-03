package server.liveops;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.Set;

/** One recurring rate window of the event calendar. Immutable. */
public record EventWindow(String name, int world, Set<DayOfWeek> days, LocalTime start, int durationMinutes,
                          int expMult, int dropMult, int mesoMult, String announce) {
    public static final int ALL_WORLDS = -1;

    public EventWindow {
        days = days.isEmpty() ? EnumSet.allOf(DayOfWeek.class) : EnumSet.copyOf(days);
    }

    public boolean appliesToWorld(int worldId) {
        return world == ALL_WORLDS || world == worldId;
    }

    /** True if {@code now} lies in [start, start + duration) of an occurrence (including ones begun on previous days). */
    public boolean isActive(ZonedDateTime now) {
        ZoneId zone = now.getZone();
        int daysBack = durationMinutes / (24 * 60) + 1;
        for (int k = 0; k <= daysBack; k++) {
            var date = now.toLocalDate().minusDays(k);
            if (!days.contains(date.getDayOfWeek())) {
                continue;
            }
            ZonedDateTime begin = date.atTime(start).atZone(zone);
            ZonedDateTime end = begin.plusMinutes(durationMinutes);
            if (!now.isBefore(begin) && now.isBefore(end)) {
                return true;
            }
        }
        return false;
    }
}
