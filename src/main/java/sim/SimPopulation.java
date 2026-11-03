package sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * How many configured bots should be online at a given hour of the day, and which ones, respecting the
 * per-map and per-channel caps. Evenings are busy, nights are quiet.
 */
public final class SimPopulation {
    private static final Logger log = LoggerFactory.getLogger(SimPopulation.class);

    /** Percent of configured bots online for hours 0..23 (server local time). */
    public static final int[] DEFAULT_CURVE = {
            25, 15, 10, 10, 10, 15, 25, 35, 45, 50, 55, 60,
            65, 65, 65, 70, 75, 85, 95, 100, 100, 90, 65, 40};

    private final int[] curve;
    private final int maxPerMap;
    private final int maxPerChannel;

    public SimPopulation(int[] curve, int maxPerMap, int maxPerChannel) {
        this.curve = curve;
        this.maxPerMap = Math.max(0, maxPerMap);
        this.maxPerChannel = Math.max(0, maxPerChannel);
    }

    /** Invalid curves (not 24 numbers) fall back to the default; values are clamped to 0..100. */
    public static int[] parseCurve(List<?> raw) {
        if (raw == null || raw.size() != 24) {
            if (raw != null && !raw.isEmpty()) {
                log.warn("SIM_POPULATION_CURVE needs exactly 24 numbers (hours 0-23), got {}; using the default", raw.size());
            }
            return DEFAULT_CURVE.clone();
        }
        int[] out = new int[24];
        try {
            for (int i = 0; i < 24; i++) {
                out[i] = Math.max(0, Math.min(100, Integer.parseInt(String.valueOf(raw.get(i)).trim())));
            }
        } catch (NumberFormatException e) {
            log.warn("SIM_POPULATION_CURVE has a non-number entry; using the default");
            return DEFAULT_CURVE.clone();
        }
        return out;
    }

    public int percentAt(int hour) {
        return curve[Math.floorMod(hour, 24)];
    }

    /** Bots allowed by the caps, in config order. */
    List<SimBotConfig> admitted(List<SimBotConfig> all) {
        Map<Integer, Integer> perChannel = new HashMap<>();
        Map<Long, Integer> perMap = new HashMap<>();
        List<SimBotConfig> out = new ArrayList<>();
        for (SimBotConfig b : all) {
            long mapKey = ((long) b.channel() << 32) | (b.homeMap() & 0xFFFFFFFFL);
            if (perMap.getOrDefault(mapKey, 0) >= maxPerMap || perChannel.getOrDefault(b.channel(), 0) >= maxPerChannel) {
                continue;
            }
            perMap.merge(mapKey, 1, Integer::sum);
            perChannel.merge(b.channel(), 1, Integer::sum);
            out.add(b);
        }
        return out;
    }

    /** How many of {@code available} bots should be online at {@code hour}. At least one while the curve is above 0. */
    public int targetCount(int hour, int available) {
        int pct = percentAt(hour);
        if (available == 0 || pct == 0) {
            return 0;
        }
        return Math.max(1, Math.min(available, Math.round(available * pct / 100f)));
    }

    /** The bots that should be online at {@code hour}: the first N admitted ones, so regulars are online the most. */
    public List<SimBotConfig> desired(List<SimBotConfig> all, int hour) {
        List<SimBotConfig> admitted = admitted(all);
        return admitted.subList(0, targetCount(hour, admitted.size()));
    }
}
