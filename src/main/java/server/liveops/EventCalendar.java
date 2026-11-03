package server.liveops;

import config.EventCalendarConfig;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Config-driven event calendar: timed windows that multiply a world's EXP / drop / meso rate.
 * Off by default ({@code event_calendar.ENABLED}). Overlapping windows never stack: for each rate the highest
 * multiplier wins. When no window is active the base rates captured at window start are restored.
 *
 * Not thread-safe by itself; {@link #tick} is meant to be called from one scheduler thread.
 */
public class EventCalendar {
    private static final Logger log = LoggerFactory.getLogger(EventCalendar.class);

    private record Applied(int baseExp, int baseDrop, int baseMeso, int expMult, int dropMult, int mesoMult) {}

    private final List<EventWindow> windows;
    private final Supplier<Map<Integer, ? extends RateSink>> worlds;
    private final Map<Integer, Applied> applied = new HashMap<>();

    public EventCalendar(List<EventWindow> windows, Supplier<Map<Integer, ? extends RateSink>> worlds) {
        this.windows = List.copyOf(windows);
        this.worlds = worlds;
    }

    public List<EventWindow> getWindows() {
        return windows;
    }

    /** Builds the calendar from config; returns null when disabled or absent. Bad entries are skipped with a warning. */
    public static EventCalendar fromConfig(EventCalendarConfig cfg, Supplier<Map<Integer, ? extends RateSink>> worlds) {
        if (cfg == null || !cfg.ENABLED) {
            return null;
        }
        List<EventWindow> parsed = parseWindows(cfg.events);
        log.info("Event calendar enabled with {} valid window(s).", parsed.size());
        return new EventCalendar(parsed, worlds);
    }

    private static volatile ZoneId hostZone;

    /**
     * Remembers the host PC's own zone. Server.init later forces the JVM default to server.TIMEZONE (GMT), after which
     * ZoneId.systemDefault() no longer reflects the machine clock, so this must be called before that happens.
     */
    public static void captureHostZone() {
        if (hostZone == null) {
            hostZone = ZoneId.systemDefault();
        }
    }

    /** The PC's own time zone (as captured at startup, else the current JVM default). */
    public static ZoneId hostZone() {
        ZoneId z = hostZone;
        return z != null ? z : ZoneId.systemDefault();
    }

    public static ZoneId parseZone(String tz) {
        if (tz == null || tz.isBlank()) {
            return hostZone();
        }
        try {
            return ZoneId.of(tz.trim());
        } catch (DateTimeException e) {
            log.warn("Event calendar: unknown TIMEZONE '{}', using the PC zone {}.", tz, hostZone());
            return hostZone();
        }
    }

    public static List<EventWindow> parseWindows(List<Map<String, Object>> entries) {
        return parseWindows(entries, log::warn);
    }

    /** As above, but every rejected entry's warning text goes to {@code warn} (tests hook in here). */
    static List<EventWindow> parseWindows(List<Map<String, Object>> entries, java.util.function.Consumer<String> warn) {
        List<EventWindow> out = new ArrayList<>();
        if (entries == null) {
            return out;
        }
        int i = 0;
        for (Map<String, Object> e : entries) {
            i++;
            try {
                out.add(parseWindow(e));
            } catch (IllegalArgumentException | NullPointerException ex) {
                warn.accept("Event calendar: ignoring entry #" + i + " (" + (e == null ? "null" : e.get("name")) + "): " + ex.getMessage());
            }
        }
        return out;
    }

    static EventWindow parseWindow(Map<String, Object> e) {
        if (e == null) {
            throw new IllegalArgumentException("empty entry");
        }
        String name = str(e.get("name"));
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("missing name");
        }
        int world = EventWindow.ALL_WORLDS;
        String w = str(e.get("world"));
        if (w != null && !w.isBlank() && !w.trim().equalsIgnoreCase("all")) {
            world = parseInt("world", w);
            if (world < 0) {
                throw new IllegalArgumentException("world must be >= 0 or 'all'");
            }
        }
        LocalTime start;
        try {
            start = LocalTime.parse(pad(str(e.get("start"))));
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("start must be HH:mm");
        }
        int duration = parseInt("duration_minutes", str(e.get("duration_minutes")));
        if (duration <= 0) {
            throw new IllegalArgumentException("duration_minutes must be > 0");
        }
        int exp = mult(e, "exp_mult");
        int drop = mult(e, "drop_mult");
        int meso = mult(e, "meso_mult");
        if (exp == 1 && drop == 1 && meso == 1) {
            throw new IllegalArgumentException("no multiplier above 1");
        }
        return new EventWindow(name.trim(), world, parseDays(e.get("days")), start, duration, exp, drop, meso, str(e.get("announce")));
    }

    private static String pad(String hhmm) {
        String s = hhmm.trim();
        return s.length() == 4 && s.charAt(1) == ':' ? "0" + s : s;
    }

    private static int mult(Map<String, Object> e, String key) {
        String s = str(e.get(key));
        if (s == null || s.isBlank()) {
            return 1;
        }
        int v = parseInt(key, s);
        if (v < 1) {
            throw new IllegalArgumentException(key + " must be >= 1");
        }
        return v;
    }

    private static int parseInt(String key, String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new IllegalArgumentException(key + " is not a whole number: " + s);
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    static Set<DayOfWeek> parseDays(Object raw) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (raw == null) {
            return days; // empty = every day
        }
        List<String> tokens = new ArrayList<>();
        if (raw instanceof Collection<?> c) {
            c.forEach(o -> tokens.add(String.valueOf(o)));
        } else {
            for (String t : raw.toString().split("[,\\s]+")) {
                tokens.add(t);
            }
        }
        for (String t : tokens) {
            String s = t.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (s.equalsIgnoreCase("daily") || s.equalsIgnoreCase("all")) {
                return EnumSet.noneOf(DayOfWeek.class);
            }
            DayOfWeek d = null;
            for (DayOfWeek cand : DayOfWeek.values()) {
                if (s.equalsIgnoreCase(cand.name()) || s.equalsIgnoreCase(cand.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))) {
                    d = cand;
                }
            }
            if (d == null) {
                throw new IllegalArgumentException("unknown day '" + s + "'");
            }
            days.add(d);
        }
        return days;
    }

    /** Highest multiplier for each rate among windows active for the given world at {@code now}: {exp, drop, meso}. */
    public int[] activeMultipliers(int worldId, ZonedDateTime now) {
        int[] m = {1, 1, 1};
        for (EventWindow w : windows) {
            if (w.appliesToWorld(worldId) && w.isActive(now)) {
                m[0] = Math.max(m[0], w.expMult());
                m[1] = Math.max(m[1], w.dropMult());
                m[2] = Math.max(m[2], w.mesoMult());
            }
        }
        return m;
    }

    /** Re-evaluates every world at {@code now}, applying or restoring rates only where the result changed. */
    public void tick(ZonedDateTime now) {
        for (Map.Entry<Integer, ? extends RateSink> en : worlds.get().entrySet()) {
            int id = en.getKey();
            RateSink sink = en.getValue();
            int[] m = activeMultipliers(id, now);
            Applied cur = applied.get(id);
            boolean active = m[0] > 1 || m[1] > 1 || m[2] > 1;
            if (!active) {
                if (cur != null) {
                    restore(id, sink, cur);
                    applied.remove(id);
                }
                continue;
            }
            if (cur != null && cur.expMult == m[0] && cur.dropMult == m[1] && cur.mesoMult == m[2]) {
                continue;
            }
            Applied next = cur != null
                    ? new Applied(cur.baseExp, cur.baseDrop, cur.baseMeso, m[0], m[1], m[2])
                    : new Applied(sink.getExpRate(), sink.getDropRate(), sink.getMesoRate(), m[0], m[1], m[2]);
            sink.setExpRate(scale(next.baseExp, m[0]));
            sink.setDropRate(scale(next.baseDrop, m[1]));
            sink.setMesoRate(scale(next.baseMeso, m[2]));
            applied.put(id, next);
            log.info("Event calendar: world {} rates -> exp x{} ({}), drop x{} ({}), meso x{} ({}).", id,
                    m[0], sink.getExpRate(), m[1], sink.getDropRate(), m[2], sink.getMesoRate());
        }
    }

    private void restore(int id, RateSink sink, Applied a) {
        sink.setExpRate(a.baseExp);
        sink.setDropRate(a.baseDrop);
        sink.setMesoRate(a.baseMeso);
        log.info("Event calendar: world {} rates restored -> exp {}, drop {}, meso {}.", id, a.baseExp, a.baseDrop, a.baseMeso);
    }

    /** Puts every world back on its base rates (used on shutdown / disable). */
    public void restoreAll() {
        for (Map.Entry<Integer, ? extends RateSink> en : worlds.get().entrySet()) {
            Applied cur = applied.remove(en.getKey());
            if (cur != null) {
                restore(en.getKey(), en.getValue(), cur);
            }
        }
    }

    private static int scale(int base, int mult) {
        return (int) Math.min(Integer.MAX_VALUE, (long) base * mult);
    }

    /** Production wiring: starts the periodic tick on the shared TimerManager. No-op when disabled. */
    public static EventCalendar start(EventCalendarConfig cfg, Supplier<List<World>> worldList) {
        return start(cfg, worldList, null);
    }

    /** As above; {@code clock} (null = real time in the configured zone) lets tests drive the real scheduler. */
    static EventCalendar start(EventCalendarConfig cfg, Supplier<List<World>> worldList,
                               java.util.function.Function<ZoneId, ZonedDateTime> clock) {
        Supplier<Map<Integer, ? extends RateSink>> sinks = () -> {
            Map<Integer, RateSink> m = new HashMap<>();
            for (World w : worldList.get()) {
                m.put(w.getId(), new WorldRateSink(w));
            }
            return m;
        };
        EventCalendar cal = fromConfig(cfg, sinks);
        if (cal == null) {
            return null;
        }
        ZoneId zone = parseZone(cfg.TIMEZONE);
        long interval = Math.max(1, cfg.CHECK_INTERVAL_SECONDS) * 1000L;
        java.util.function.Function<ZoneId, ZonedDateTime> now = clock != null ? clock : ZonedDateTime::now;
        log.info("Event calendar: zone={} (now {} there; blank TIMEZONE = this PC's zone), check every {}s, windows={}",
                zone, now.apply(zone).toLocalTime().withNano(0), interval / 1000, cal.getWindows());
        server.TimerManager.getInstance().register(() -> {
            try {
                cal.tick(now.apply(zone));
            } catch (RuntimeException ex) {
                log.warn("Event calendar tick failed", ex);
            }
        }, interval, 1000L);
        return cal;
    }

    private record WorldRateSink(World w) implements RateSink {
        public int getExpRate() { return w.getExpRate(); }
        public void setExpRate(int r) { w.setExpRate(r); }
        public int getDropRate() { return w.getDropRate(); }
        public void setDropRate(int r) { w.setDropRate(r); }
        public int getMesoRate() { return w.getMesoRate(); }
        public void setMesoRate(int r) { w.setMesoRate(r); }
    }
}
