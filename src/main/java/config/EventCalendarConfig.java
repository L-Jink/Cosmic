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

    /** Minutes before an event window starts at which "starting soon" notices go out (also used for maintenance). */
    public List<Integer> NOTICE_MINUTES = List.of(10, 5, 1);
    /** serverNotice type used for all liveops broadcasts (0 notice, 5 pink, 6 light blue). */
    public int NOTICE_TYPE = 6;
    public String MSG_EVENT_SOON = "[Event] {name} starts in {minutes} minute(s)!";
    public String MSG_EVENT_START = "[Event] {name} is now live!";
    public String MSG_EVENT_END = "[Event] {name} has ended. Thanks for playing!";

    /** Maintenance countdown: extra notices (seconds before shutdown) in addition to NOTICE_MINUTES. */
    public List<Integer> MAINTENANCE_NOTICE_SECONDS = List.of(30, 10);
    /** Seconds between blocking new logins (T-0) and the clean shutdown. */
    public int MAINTENANCE_SHUTDOWN_GRACE_SECONDS = 5;
    public String MSG_MAINT_START = "[Maintenance] The server will go down for maintenance in {time}. Please finish what you are doing and log out safely.";
    public String MSG_MAINT_WARN = "[Maintenance] The server will go down for maintenance in {time}.";
    public String MSG_MAINT_NOW = "[Maintenance] The server is going down for maintenance now. New logins are disabled.";
    public String MSG_MAINT_CANCEL = "[Maintenance] The scheduled maintenance has been cancelled.";
}
