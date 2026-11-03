package sim;

import com.esotericsoftware.yamlbeans.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The phrase bank idle bots draw their chatter from (config/sim_phrases.yaml). */
public final class SimPhrases {
    private static final Logger log = LoggerFactory.getLogger(SimPhrases.class);
    static final List<String> FALLBACK = List.of("hi all", "hello", "lf party", "brb", "lol", "nice");
    /** v83 chat line limit is 70 chars. */
    static final int MAX_LENGTH = 70;

    private final List<String> lines;

    public SimPhrases(List<String> lines) {
        List<String> clean = new ArrayList<>();
        for (String l : lines) {
            if (l != null && !l.isBlank() && l.strip().length() <= MAX_LENGTH) {
                clean.add(l.strip());
            }
        }
        this.lines = clean.isEmpty() ? FALLBACK : List.copyOf(clean);
    }

    public static SimPhrases load(String file) {
        try (var reader = new YamlReader(Files.newBufferedReader(Path.of(file)))) {
            Object root = reader.read();
            if (root instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) {
                    out.add(o == null ? null : o.toString());
                }
                return new SimPhrases(out);
            }
            log.warn("Sim phrase file {} is not a list, using built-in phrases", file);
        } catch (IOException e) {
            log.warn("Could not read sim phrase file {} ({}), using built-in phrases", file, e.toString());
        }
        return new SimPhrases(FALLBACK);
    }

    public int size() {
        return lines.size();
    }

    public List<String> all() {
        return lines;
    }

    /** Random line, never equal to {@code previous} (when there is more than one line). */
    public String pick(Random random, String previous) {
        if (lines.size() == 1) {
            return lines.get(0);
        }
        String s;
        do {
            s = lines.get(random.nextInt(lines.size()));
        } while (s.equals(previous));
        return s;
    }
}
