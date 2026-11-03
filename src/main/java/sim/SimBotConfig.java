package sim;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One configured resident from the {@code SIM_BOTS} list in config.yaml.
 *
 * @param name    character name (unique, 4-12 letters/digits)
 * @param job     v83 job id (e.g. 0 beginner, 110 fighter, 310 hunter)
 * @param level   character level
 * @param homeMap map id the bot lives in
 * @param channel 1-based channel number on world 0
 * @param gender  0 male, 1 female
 */
public record SimBotConfig(String name, int job, int level, int homeMap, int channel, int gender) {
    private static final Logger log = LoggerFactory.getLogger(SimBotConfig.class);

    /**
     * Parses the raw YAML list. Invalid entries are logged and skipped so one typo does not stop the server.
     */
    public static List<SimBotConfig> parse(List<Map<String, Object>> raw) {
        List<SimBotConfig> bots = new ArrayList<>();
        if (raw == null) {
            return bots;
        }
        for (Map<String, Object> entry : raw) {
            try {
                SimBotConfig bot = fromMap(entry);
                if (bots.stream().anyMatch(b -> b.name().equalsIgnoreCase(bot.name()))) {
                    log.warn("Sim bot {} is listed twice in SIM_BOTS, ignoring the duplicate", bot.name());
                    continue;
                }
                bots.add(bot);
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring invalid SIM_BOTS entry {}: {}", entry, e.getMessage());
            }
        }
        return bots;
    }

    static SimBotConfig fromMap(Map<String, Object> m) {
        String name = str(m, "name");
        if (name == null || !name.matches("[A-Za-z0-9]{4,12}")) {
            throw new IllegalArgumentException("name must be 4-12 letters/digits");
        }
        int level = num(m, "level", 1);
        if (level < 1 || level > 200) {
            throw new IllegalArgumentException("level must be 1-200");
        }
        int homeMap = num(m, "homeMap", -1);
        if (homeMap <= 0) {
            throw new IllegalArgumentException("homeMap is required");
        }
        int channel = num(m, "channel", 1);
        if (channel < 1) {
            throw new IllegalArgumentException("channel must be >= 1");
        }
        int gender = num(m, "gender", 0);
        if (gender != 0 && gender != 1) {
            throw new IllegalArgumentException("gender must be 0 or 1");
        }
        return new SimBotConfig(name, num(m, "job", 0), level, homeMap, channel, gender);
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString().trim();
    }

    private static int num(Map<String, Object> m, String key, int def) {
        String v = str(m, key);
        if (v == null || v.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " is not a number: " + v);
        }
    }
}
