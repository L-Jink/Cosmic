package server.liveops;

import config.EventCalendarConfig;
import config.YamlConfig;
import net.server.Server;
import net.server.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.TimerManager;
import tools.PacketCreator;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/** Production wiring for {@link MaintenanceCountdown}: world broadcasts, login gate, clean shutdown. */
public final class Maintenance {
    private static final Logger log = LoggerFactory.getLogger(Maintenance.class);

    /** Login failure reason shown to the client while logins are blocked ("Cannot process so many connections"). */
    public static final int LOGIN_BLOCKED_REASON = 10;

    private static MaintenanceCountdown current;
    private static ScheduledFuture<?> ticker;

    private Maintenance() {}

    public static EventCalendarConfig cfg() {
        EventCalendarConfig c = YamlConfig.config.event_calendar;
        return c != null ? c : new EventCalendarConfig();
    }

    /** True once a maintenance countdown reached T-0: new logins must be refused. */
    public static synchronized boolean isLoginBlocked() {
        return current != null && current.isLoginBlocked();
    }

    /** Broadcast a notice to every player in every world. */
    public static void broadcastAll(String message) {
        log.info("World notice: {}", message);
        var packet = PacketCreator.serverNotice(cfg().NOTICE_TYPE, message);
        for (World w : Server.getInstance().getWorlds()) {
            w.broadcastPacket(packet);
        }
    }

    /** Broadcast a notice to one world, or all worlds when {@code worldId} is {@link EventWindow#ALL_WORLDS}. */
    public static void broadcast(int worldId, String message) {
        if (worldId == EventWindow.ALL_WORLDS) {
            broadcastAll(message);
            return;
        }
        log.info("World notice (world {}): {}", worldId, message);
        var packet = PacketCreator.serverNotice(cfg().NOTICE_TYPE, message);
        for (World w : Server.getInstance().getWorlds()) {
            if (w.getId() == worldId) {
                w.broadcastPacket(packet);
            }
        }
    }

    /** Starts a countdown; returns false if one is already running. */
    public static synchronized boolean start(Duration total, String startedBy) {
        if (current != null && current.getState() != MaintenanceCountdown.State.CANCELLED) {
            return false;
        }
        EventCalendarConfig c = cfg();
        log.info("Maintenance countdown of {}s started by {}.", total.toSeconds(), startedBy);
        var msgs = new MaintenanceCountdown.Messages(c.MSG_MAINT_START, c.MSG_MAINT_WARN, c.MSG_MAINT_NOW, c.MSG_MAINT_CANCEL);
        MaintenanceCountdown mine = new MaintenanceCountdown(Clock.systemUTC(), total,
                MaintenanceCountdown.offsets(c.NOTICE_MINUTES, c.MAINTENANCE_NOTICE_SECONDS),
                Duration.ofSeconds(Math.max(0, c.MAINTENANCE_SHUTDOWN_GRACE_SECONDS)), msgs,
                Maintenance::broadcastAll,
                () -> log.info("Maintenance: T-0 reached, new logins are now blocked."),
                () -> {
                    log.info("Maintenance: shutting down cleanly.");
                    Server.getInstance().shutdown(false).run();
                });
        current = mine;
        ticker = TimerManager.getInstance().register(() -> {
            try {
                mine.tick();
            } catch (RuntimeException e) {
                log.warn("Maintenance tick failed", e);
            }
        }, 1000, 1000);
        return true;
    }

    public static synchronized boolean cancel() {
        if (current == null || !current.cancel()) {
            return false;
        }
        if (ticker != null) {
            ticker.cancel(false);
            ticker = null;
        }
        return true;
    }
}
