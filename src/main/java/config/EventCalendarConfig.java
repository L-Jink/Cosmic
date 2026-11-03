package config;

import java.util.List;
import java.util.Map;

/**
 * Raw {@code event_calendar:} section of config.yaml. Entries are kept as loose maps on purpose so that one
 * malformed entry is skipped (with a warning) by {@code server.liveops.EventCalendar} instead of breaking the whole config.
 */
public class EventCalendarConfig {
    public boolean ENABLED = false;
    /** IANA zone id (e.g. "Asia/Kuala_Lumpur"); blank = server machine's zone. */
    public String TIMEZONE = "";
    public int CHECK_INTERVAL_SECONDS = 15;
    public List<Map<String, Object>> events;
}
