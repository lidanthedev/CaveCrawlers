package me.lidan.cavecrawlers.test;

import me.lidan.cavecrawlers.CaveCrawlers;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.mockito.MockedStatic;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** Minimal MockBukkit context for production classes that require CaveCrawlers.getInstance(). */
public final class MockCaveCrawlers implements AutoCloseable {
    private final ServerMock server;
    private final CaveCrawlers plugin;
    private final MockedStatic<JavaPlugin> javaPlugin;

    private MockCaveCrawlers(ServerMock server, CaveCrawlers plugin, MockedStatic<JavaPlugin> javaPlugin) {
        this.server = server;
        this.plugin = plugin;
        this.javaPlugin = javaPlugin;
    }

    public static MockCaveCrawlers start() throws Exception {
        ServerMock server = MockBukkit.mock();
        CaveCrawlers plugin = allocatePlugin(server);
        MockedStatic<JavaPlugin> javaPlugin = mockStatic(JavaPlugin.class);
        javaPlugin.when(() -> JavaPlugin.getPlugin(CaveCrawlers.class)).thenReturn(plugin);
        javaPlugin.when(() -> JavaPlugin.getProvidingPlugin(any())).thenReturn(plugin);
        return new MockCaveCrawlers(server, plugin, javaPlugin);
    }

    public ServerMock server() {
        return server;
    }

    public CaveCrawlers plugin() {
        return plugin;
    }

    @Override
    public void close() throws Exception {
        javaPlugin.close();
        MockBukkit.unmock();
    }

    private static CaveCrawlers allocatePlugin(ServerMock server) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        CaveCrawlers plugin = (CaveCrawlers) unsafe.allocateInstance(CaveCrawlers.class);
        setField(plugin, JavaPlugin.class, "server", server);
        setField(plugin, JavaPlugin.class, "pluginMeta", new PluginDescriptionFile(
                "CaveCrawlers", "test", CaveCrawlers.class.getName()));
        File dataFolder = new File("build/test-plugin");
        dataFolder.mkdirs();
        setField(plugin, JavaPlugin.class, "dataFolder", dataFolder);
        setField(plugin, JavaPlugin.class, "newConfig", new YamlConfiguration());
        setField(plugin, JavaPlugin.class, "logger", Logger.getLogger("cavecrawlers-test"));
        setField(plugin, JavaPlugin.class, "isEnabled", true);
        PluginLoader loader = mock(PluginLoader.class);
        when(loader.createRegisteredListeners(any(Listener.class), eq(plugin))).thenReturn(java.util.Map.of());
        setField(plugin, JavaPlugin.class, "loader", loader);
        return plugin;
    }

    private static void setField(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
