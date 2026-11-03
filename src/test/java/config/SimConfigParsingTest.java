package config;

import org.junit.jupiter.api.Test;
import sim.SimBotConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sim section of config.yaml (lives in package config to reach the package-private loader). */
class SimConfigParsingTest {
    @Test
    void shippedConfigIsOffByDefaultWithFiveHenesysBots() throws IOException {
        YamlConfig cfg = YamlConfig.load(Path.of(YamlConfig.CONFIG_FILE_NAME), null, null);

        assertFalse(cfg.server.SIM_PLAYERS_ENABLED);
        List<SimBotConfig> bots = SimBotConfig.parse(cfg.server.SIM_BOTS);
        assertEquals(5, bots.size());
        assertTrue(bots.stream().allMatch(b -> b.homeMap() == 100000000));
        assertEquals(5, bots.stream().map(SimBotConfig::name).distinct().count());
    }

    @Test
    void localOverrideCanEnableSims() throws IOException {
        Path local = Files.createTempFile("sim-local", ".yaml");
        try {
            Files.writeString(local, "server:\n  SIM_PLAYERS_ENABLED: true\n");
            YamlConfig cfg = YamlConfig.load(Path.of(YamlConfig.CONFIG_FILE_NAME), local, null);

            assertTrue(cfg.server.SIM_PLAYERS_ENABLED);
            assertEquals(5, SimBotConfig.parse(cfg.server.SIM_BOTS).size());
        } finally {
            Files.deleteIfExists(local);
        }
    }
}
