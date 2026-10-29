package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.objects.ConfigLoader;
import me.lidan.cavecrawlers.objects.ConfigMessage;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.utils.BoostedConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigLoaderCoverageTest {
    private MockCaveCrawlers context;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    void registerItemsSkipsVersionAndInvalidObjects() throws Exception {
        File directory = Files.createTempDirectory("cavecrawlers-loader").toFile();
        Map<String, ConfigMessage> registered = new HashMap<>();
        ConfigLoader<ConfigMessage> loader = new ConfigLoader<>(ConfigMessage.class, directory) {
            @Override public void register(String key, ConfigMessage value) { registered.put(key, value); }
        };
        BoostedConfiguration configuration = mock(BoostedConfiguration.class);
        ConfigMessage message = new ConfigMessage("hello");
        when(configuration.getKeys(false)).thenReturn(Set.of("version", "good", "bad"));
        when(configuration.getObject(eq("good"), eq(ConfigMessage.class))).thenReturn(message);
        when(configuration.getObject(eq("bad"), eq(ConfigMessage.class))).thenReturn(null);

        Set<String> result = loader.registerItemsFromConfig(configuration);

        assertEquals(Set.of("good"), result);
        assertEquals(Map.of("good", message), registered);
        assertTrue(loader.getConfigMap().isEmpty());
    }

    @Test
    void clearRemovesSourceMapWithoutDeletingFiles() throws Exception {
        File directory = Files.createTempDirectory("cavecrawlers-loader").toFile();
        ConfigLoader<ConfigMessage> loader = new ConfigLoader<>(ConfigMessage.class, directory) {
            @Override public void register(String key, ConfigMessage value) { }
        };
        File source = new File(directory, "messages.yml");
        source.createNewFile();
        loader.getConfigMap().put("message", source);

        loader.clear();

        assertTrue(loader.getConfigMap().isEmpty());
        assertTrue(source.exists());
    }

    @Test
    void loadingMissingDirectoryIsSafeAndCreatesItWhenRegisteringFolder() throws Exception {
        File directory = new File("build/config-loader-missing-" + System.nanoTime());
        ConfigLoader<ConfigMessage> loader = new ConfigLoader<>(ConfigMessage.class, directory) {
            @Override public void register(String key, ConfigMessage value) { }
        };

        loader.load();
        assertFalse(directory.exists(), "load() should not create a missing root by itself");
        loader.registerItemsFromFolder(directory);
        assertTrue(directory.exists());
    }
}
