package me.lidan.cavecrawlers.coverage;

import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import me.lidan.cavecrawlers.utils.ColorUtils;
import me.lidan.cavecrawlers.utils.Cooldown;
import me.lidan.cavecrawlers.utils.MessageUtils;
import me.lidan.cavecrawlers.utils.MiniMessageUtils;
import me.lidan.cavecrawlers.utils.Range;
import me.lidan.cavecrawlers.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityCoverageTest {
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
    void rangeParsesSingleAndNegativeBoundsAndIteratesInclusively() {
        assertEquals("-2-2", new Range("-2-2").toString());
        assertEquals(List.of(-2L, -1L, 0L, 1L, 2L), new Range("-2-2").getRangeAsList());
        assertEquals(List.of(5L, 6L), new Range(5, 6).getRangeAsList());
        assertTrue(new Range(5, 6).isInRange(5));
        assertTrue(new Range(5, 6).isInRange(6));
        assertFalse(new Range(5, 6).isInRange(7));
    }

    @Test
    void reversedRangeIsEmptyAndIteratorExhaustionIsExplicit() {
        Range range = new Range(4, 2);
        assertTrue(range.getRangeAsList().isEmpty());
        assertFalse(range.iterator().hasNext());
        assertThrows(java.util.NoSuchElementException.class, () -> range.iterator().next());
    }

    @Test
    void formattingAndPluralHelpersCoverCommonBoundaries() {
        assertEquals("Hello World", StringUtils.setTitleCase("hELLO wORLD"));
        assertEquals("1,234.57", StringUtils.getNumberFormat(1234.567));
        assertEquals("apple", StringUtils.getPlural("apple", 1));
        assertEquals("books", StringUtils.getPlural("book", 2));
        assertEquals("children", StringUtils.getPlural("child", 2));
        assertEquals("42", StringUtils.getNumberWithoutDot(42.75));
    }

    @Test
    void miniMessageConversionAndProgressBarPreservePlainText() {
        Component component = MiniMessageUtils.miniMessage("<red>Hello <name>", java.util.Map.of("name", "Cave"));
        assertEquals("Hello Cave", MiniMessageUtils.componentToString(component));
        assertEquals("Hello Cave", PlainTextComponentSerializer.plainText().serialize(component));
        assertNotNull(MiniMessageUtils.progressBar(5, 10, 10));
        assertEquals("Hello", MiniMessageUtils.componentToString(MiniMessageUtils.miniMessageString("Hello")));
    }

    @Test
    void colorHelpersRoundTripKnownMinecraftColors() {
        assertEquals(16, ColorUtils.getAllMinecraftColors().size());
        assertEquals("#ff0000", ColorUtils.getHexColor(Color.RED));
        assertEquals("RED", ColorUtils.getMinecraftColorFromBukkitColor(Color.RED));
        assertEquals("unknown", ColorUtils.getMinecraftColorFromBukkitColor(Color.fromRGB(1, 2, 3)));
    }

    @Test
    void cooldownRemovesExpiredEntriesAndRejectsUnsetDuration() {
        Cooldown<String> cooldown = new Cooldown<>(100L);
        cooldown.setCooldown("old", System.currentTimeMillis() - 200);
        assertTrue(cooldown.isCooldownFinished("old"));
        assertEquals(0, cooldown.getCooldown("old"));
        cooldown.startCooldown("new");
        assertTrue(cooldown.getCooldown("new") > 0);
        cooldown.resetCooldown("new");
        assertTrue(cooldown.isCooldownFinished("new"));
        assertThrows(IllegalArgumentException.class, () -> new Cooldown<String>().isCooldownFinished("key"));
    }

    @Test
    void messageCenteringHandlesNullAndFormattedInput() {
        assertEquals(Component.empty(), MessageUtils.CenteredMessageWithComponent(null));
        assertEquals(Component.empty(), MessageUtils.CenteredMessageWithMiniMessage(""));
        assertTrue(MessageUtils.spaceLine().contains("<dark_gray>"));
        assertTrue(MessageUtils.CenteredMessageWithMinecraftSymbols("&aHello").contains(ChatColor.GREEN.toString()));
    }
}
