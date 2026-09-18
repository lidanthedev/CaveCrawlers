package me.lidan.cavecrawlers.coverage;

import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import me.lidan.cavecrawlers.gui.PaginatedRowGui;
import me.lidan.cavecrawlers.test.MockCaveCrawlers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GuiCoverageTest {
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
    void rowLayoutMapsEverySupportedItemCountToStableSlots() {
        TestRow row = new TestRow(mock(Gui.class));

        assertEquals(List.of(5), row.getLayoutForItems(1));
        assertEquals(List.of(4, 6), row.getLayoutForItems(2));
        assertEquals(List.of(4, 5, 6), row.getLayoutForItems(3));
        assertEquals(List.of(2, 4, 6, 8), row.getLayoutForItems(4));
        assertEquals(List.of(3, 4, 5, 6, 7), row.getLayoutForItems(5));
        assertEquals(List.of(2, 3, 4, 6, 7, 8), row.getLayoutForItems(6));
        assertEquals(List.of(2, 3, 4, 5, 6, 7, 8), row.getLayoutForItems(7));
        assertEquals(List.of(2, 3, 4, 5, 6, 7, 8), row.getLayoutForItems(20));
    }

    @Test
    void rowPaginationStopsAtFirstAndLastPage() {
        TestRow row = new TestRow(mock(Gui.class));
        row.addItems(8);

        row.previous();
        assertEquals(0, row.page());
        assertEquals(0, row.updates());

        row.next();
        assertEquals(1, row.page());
        assertEquals(1, row.updates());
        row.next();
        assertEquals(1, row.page());
        assertEquals(1, row.updates());

        row.previous();
        assertEquals(0, row.page());
        assertEquals(2, row.updates());
    }

    private static final class TestRow extends PaginatedRowGui {
        private int updates;
        private TestRow(Gui gui) { super(gui); }

        private void addItems(int count) {
            for (int i = 0; i < count; i++) items.add(mock(GuiItem.class));
        }

        @Override public void updateItems() { updates++; }
        private int page() { return currentPage; }
        private int updates() { return updates; }
    }
}
