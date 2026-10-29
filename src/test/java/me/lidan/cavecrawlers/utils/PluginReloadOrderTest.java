package me.lidan.cavecrawlers.utils;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PluginReloadOrderTest {
    @Test void reloadsSharedProvidersBeforeConsumers() {
        Map<String, List<String>> dependencies = new LinkedHashMap<>();
        dependencies.put("EventLogger", List.of("Cave", "Fishing", "Griffin"));
        dependencies.put("Fishing", List.of("Cave", "absent"));
        dependencies.put("Griffin", List.of("Cave"));
        dependencies.put("Cave", List.of());
        assertEquals(List.of("Cave", "Fishing", "Griffin", "EventLogger"), PluginReloadOrder.ordered(dependencies));
    }
    @Test void optionalCyclesReloadEachPluginOnce() {
        var result = PluginReloadOrder.ordered(Map.of("A", List.of("B"), "B", List.of("A")));
        assertEquals(2, result.size());
        assertEquals(Set.of("A", "B"), Set.copyOf(result));
    }
}
