package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.levels.LevelInfo;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.utils.CustomConfig;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LevelInfoCoverageTest {
    private MockCaveCrawlers context;
    private Path file;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        file = Files.createTempFile("level-info", ".yml");
    }

    @AfterEach
    void tearDown() throws Exception {
        Files.deleteIfExists(file);
        context.close();
    }

    @Test
    void levelInfoRoundTripsLevelAndColor() {
        LevelInfo original = new LevelInfo(12, ChatColor.AQUA);
        LevelInfo copy = LevelInfo.deserialize(original.serialize());

        assertEquals(12, copy.getLevel());
        assertEquals(ChatColor.AQUA, copy.getColor());
    }

    @Test
    void playerLevelInfoUsesConfiguredValuesAndNullForUnknownPlayer() {
        CustomConfig config = new CustomConfig(file.toFile());
        String playerId = UUID.randomUUID().toString();
        config.set("players." + playerId + ".level", 4);
        config.set("players." + playerId + ".color", "LIGHT_PURPLE");

        LevelInfo info = LevelInfo.getPlayerLevelInfo(playerId, config);

        assertEquals(4, info.getLevel());
        assertEquals(ChatColor.LIGHT_PURPLE, info.getColor());
        assertNull(LevelInfo.getPlayerLevelInfo("missing", config));
    }

    @Test
    void missingPlayerFieldsUseDocumentedDefaults() {
        YamlConfiguration config = new YamlConfiguration();
        String playerId = UUID.randomUUID().toString();
        config.set("players." + playerId, new YamlConfiguration());

        LevelInfo info = LevelInfo.getPlayerLevelInfo(playerId, asCustomConfig(config));

        assertEquals(1, info.getLevel());
        assertEquals(ChatColor.GRAY, info.getColor());
    }

    private CustomConfig asCustomConfig(YamlConfiguration source) {
        CustomConfig config = new CustomConfig(file.toFile());
        config.set("players", source.getConfigurationSection("players"));
        return config;
    }
}
