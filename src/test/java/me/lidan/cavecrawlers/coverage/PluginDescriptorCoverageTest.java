package me.lidan.cavecrawlers.coverage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDescriptorCoverageTest {
    @Test
    void pluginDescriptorDeclaresLifecycleDependenciesAndAdminPermission() {
        InputStream resource = getClass().getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(resource);
        YamlConfiguration plugin = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8));

        assertEquals("CaveCrawlers", plugin.getString("name"));
        assertEquals("me.lidan.cavecrawlers.CaveCrawlers", plugin.getString("main"));
        assertEquals(List.of("Vault", "ProtocolLib", "MythicMobs"), plugin.getStringList("depend"));
        assertTrue(plugin.isConfigurationSection("permissions.cavecrawlers.admin"));
    }
}
