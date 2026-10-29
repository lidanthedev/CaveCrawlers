package me.lidan.cavecrawlers.utils;

import java.util.*;

/** Providers reload before consumers; optional dependency cycles are visited once. */
public final class PluginReloadOrder {
    private PluginReloadOrder() { }
    public static List<String> ordered(Map<String, List<String>> dependencies) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String name : dependencies.keySet()) visit(name, dependencies, visited, result);
        return List.copyOf(result);
    }
    private static void visit(String name, Map<String, List<String>> dependencies, Set<String> visited,
                              List<String> result) {
        if (!dependencies.containsKey(name) || !visited.add(name)) return;
        for (String dependency : dependencies.get(name)) visit(dependency, dependencies, visited, result);
        result.add(name);
    }
}
