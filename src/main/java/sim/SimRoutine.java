package sim;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Random;

/** Pure planning of idle walking, so it can be unit tested without a server. */
final class SimRoutine {
    private SimRoutine() {
    }

    static final int MIN_STEP = 40;
    static final int MAX_STEP = 180;
    /** Largest height difference a step may have; keeps bots on their own platform instead of dropping off. */
    static final int MAX_STEP_RISE = 24;
    /** Walk speed, milliseconds per pixel (about 125 px/s). */
    static final int MS_PER_PIXEL = 8;
    private static final int EDGE_MARGIN = 20;

    record Move(SimBackend.Ground to, boolean right, int durationMs) {
    }

    @FunctionalInterface
    interface GroundLookup {
        /** Foothold surface at x, looking down from y; null when there is none. */
        SimBackend.Ground at(int x, int y);
    }

    /** @return a short walk to the left or right, or null if no valid spot was found this time. */
    static Move planWalk(Point pos, Rectangle bounds, GroundLookup ground, Random random) {
        int minX = bounds.x + EDGE_MARGIN;
        int maxX = bounds.x + bounds.width - EDGE_MARGIN;
        for (int attempt = 0; attempt < 4; attempt++) {
            boolean right = random.nextBoolean();
            int step = MIN_STEP + random.nextInt(MAX_STEP - MIN_STEP + 1);
            int x = pos.x + (right ? step : -step);
            if (x < minX || x > maxX) {
                right = !right;
                x = pos.x + (right ? step : -step);
                if (x < minX || x > maxX) {
                    continue;
                }
            }
            SimBackend.Ground g = ground.at(x, pos.y - 5);
            if (g == null || Math.abs(g.y() - pos.y) > MAX_STEP_RISE || !bounds.contains(g.x(), g.y())) {
                continue;
            }
            return new Move(g, right, Math.abs(g.x() - pos.x) * MS_PER_PIXEL);
        }
        return null;
    }
}
