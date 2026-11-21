package me.lidan.cavecrawlers.commands.completions;

import me.lidan.cavecrawlers.items.abilities.LaserAbility;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.bukkit.Particle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AbilityIDCompletionsTest {
    private MockCaveCrawlers context;
    private LaserAbility laser;

    @BeforeEach
    void setUp() throws Exception {
        context = MockCaveCrawlers.start();
        laser = new LaserAbility("Laser", "Shoot Laser", 10, 100, Particle.END_ROD, 100, 0.1, 16);
    }

    @AfterEach
    void tearDown() throws Exception {
        context.close();
    }

    @Test
    void suggestsAllKeysAfterOpeningBrace() {
        List<String> suggestions = AbilityIDCompletions.suggestSettings("BASIC_LASER{", laser);
        assertTrue(suggestions.containsAll(List.of("BASIC_LASER{cost:", "BASIC_LASER{cooldown:",
                "BASIC_LASER{name:", "BASIC_LASER{description:", "BASIC_LASER{particle:")));
    }

    @Test
    void filtersByTypedPrefixAndSkipsUsedKeys() {
        assertEquals(List.of("BASIC_LASER{cost:5,cooldown:"),
                AbilityIDCompletions.suggestSettings("BASIC_LASER{cost:5,co", laser));
        assertEquals(List.of("BASIC_LASER{\"cost\":5,cooldown:"),
                AbilityIDCompletions.suggestSettings("BASIC_LASER{\"cost\":5,co", laser));
    }

    @Test
    void suggestsEnumValues() {
        List<String> suggestions = AbilityIDCompletions.suggestSettings("BASIC_LASER{particle:FLA", laser);
        assertTrue(suggestions.contains("BASIC_LASER{particle:FLAME"));
        assertTrue(suggestions.stream().allMatch(s -> s.startsWith("BASIC_LASER{particle:FLA")));
        assertEquals(List.of("BASIC_LASER{crit:true", "BASIC_LASER{crit:false"),
                AbilityIDCompletions.suggestSettings("BASIC_LASER{crit:", laser));
    }

    @Test
    void suggestsNothingInsideStringsNestedValuesOrFreeFormValues() {
        assertEquals(List.of(), AbilityIDCompletions.suggestSettings("BASIC_LASER{name:\"a,", laser));
        assertEquals(List.of(), AbilityIDCompletions.suggestSettings("BASIC_LASER{allowedActions:[", laser));
        assertEquals(List.of(), AbilityIDCompletions.suggestSettings("BASIC_LASER{cost:", laser));
        assertEquals(List.of(), AbilityIDCompletions.suggestSettings("BASIC_LASER{cost:5}", laser));
    }
}
