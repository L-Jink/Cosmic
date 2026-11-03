package server.liveops;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * Pure state machine for the server-maintenance flow: notices on the way down, login block at T-0, shutdown after a
 * short grace. Driven by {@link #tick()} (once a second in production) against an injectable {@link Clock}.
 */
public class MaintenanceCountdown {
    public enum State { COUNTING, LOGIN_BLOCKED, DONE, CANCELLED }

    public record Messages(String start, String warn, String now, String cancel) {}

    private final Clock clock;
    private final Instant target;
    private final Duration grace;
    private final Consumer<String> broadcast;
    private final Runnable onLoginBlock;
    private final Runnable onShutdown;
    private final Messages msgs;
    /** Seconds-before-target at which a warning is still due, descending. */
    private final List<Long> pending;
    private State state = State.COUNTING;

    public MaintenanceCountdown(Clock clock, Duration total, Collection<Long> noticeOffsetsSeconds, Duration grace, Messages msgs,
                                Consumer<String> broadcast, Runnable onLoginBlock, Runnable onShutdown) {
        this.clock = clock;
        this.target = clock.instant().plus(total);
        this.grace = grace;
        this.msgs = msgs;
        this.broadcast = broadcast;
        this.onLoginBlock = onLoginBlock;
        this.onShutdown = onShutdown;
        this.pending = noticeOffsets(total.toSeconds(), noticeOffsetsSeconds);
        broadcast.accept(msgs.start().replace("{time}", formatTime(total.toSeconds())));
    }

    /** Offsets (seconds before T-0) that get a warning for a countdown of {@code totalSeconds}: strictly inside it, descending. */
    public static List<Long> noticeOffsets(long totalSeconds, Collection<Long> candidates) {
        TreeSet<Long> set = new TreeSet<>(Comparator.reverseOrder());
        for (long c : candidates) {
            if (c > 0 && c < totalSeconds) {
                set.add(c);
            }
        }
        return new ArrayList<>(set);
    }

    /** Converts minutes + seconds config lists into one seconds list. */
    public static List<Long> offsets(Collection<Integer> minutes, Collection<Integer> seconds) {
        List<Long> out = new ArrayList<>();
        if (minutes != null) {
            minutes.forEach(m -> out.add(m * 60L));
        }
        if (seconds != null) {
            seconds.forEach(s -> out.add((long) s));
        }
        return out;
    }

    public static String formatTime(long secs) {
        long m = secs / 60;
        long s = secs % 60;
        StringBuilder sb = new StringBuilder();
        if (m > 0) {
            sb.append(m).append(m == 1 ? " minute" : " minutes");
        }
        if (s > 0 || m == 0) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s).append(s == 1 ? " second" : " seconds");
        }
        return sb.toString();
    }

    public synchronized State getState() {
        return state;
    }

    public synchronized boolean isLoginBlocked() {
        return state == State.LOGIN_BLOCKED || state == State.DONE;
    }

    public synchronized void tick() {
        if (state == State.DONE || state == State.CANCELLED) {
            return;
        }
        Instant now = clock.instant();
        long remaining = (long) Math.ceil(Duration.between(now, target).toMillis() / 1000.0);
        if (state == State.COUNTING) {
            // fire only the tightest warning that is due; coarser ones skipped by a late tick are dropped
            boolean due = false;
            while (remaining > 0 && !pending.isEmpty() && pending.get(0) >= remaining) {
                pending.remove(0);
                due = true;
            }
            if (due) {
                broadcast.accept(msgs.warn().replace("{time}", formatTime(remaining)));
            }
            if (remaining <= 0) {
                state = State.LOGIN_BLOCKED;
                onLoginBlock.run();
                broadcast.accept(msgs.now());
            }
        }
        if (state == State.LOGIN_BLOCKED && !now.isBefore(target.plus(grace))) {
            state = State.DONE;
            onShutdown.run();
        }
    }

    /** Aborts a countdown that has not yet blocked logins. Returns false if too late. */
    public synchronized boolean cancel() {
        if (state != State.COUNTING) {
            return false;
        }
        state = State.CANCELLED;
        broadcast.accept(msgs.cancel());
        return true;
    }
}
