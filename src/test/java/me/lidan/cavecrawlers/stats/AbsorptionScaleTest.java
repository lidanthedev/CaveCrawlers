package me.lidan.cavecrawlers.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbsorptionScaleTest {
    @Test
    void largeNativePoolShrinksProportionallyWithinOneRow() {
        AbsorptionScale scale = new AbsorptionScale();
        assertEquals(20, scale.update(3200));
        assertEquals(10, scale.update(1600));
        assertEquals(5, scale.update(800));
        assertEquals(0, scale.update(0));
    }

    @Test
    void depletionStartsANewPoolAndIncreasingAbsorptionRaisesThePeak() {
        AbsorptionScale scale = new AbsorptionScale();
        scale.update(3200);
        assertEquals(10, scale.update(1600));
        assertEquals(20, scale.update(6400));
        assertEquals(10, scale.update(3200));
        scale.update(0);
        assertEquals(20, scale.update(4));
        assertEquals(10, scale.update(2));
    }

    @Test
    void repeatedSnapshotsDoNotRefillTheDisplay() {
        AbsorptionScale scale = new AbsorptionScale();
        scale.update(3200);
        assertEquals(10, scale.update(1600));
        assertEquals(10, scale.update(1600));
    }

    @Test
    void handlesFractionalExtremeAndInvalidAmountsWithoutOverflow() {
        AbsorptionScale scale = new AbsorptionScale();
        assertEquals(20, scale.update(Float.MAX_VALUE));
        assertEquals(10, scale.update(Float.MAX_VALUE / 2));
        assertEquals(0, scale.update(Float.NaN));
        assertEquals(0, scale.update(Float.POSITIVE_INFINITY));
        assertEquals(0, scale.update(-1));
        assertEquals(20, scale.update(0.25f));
        assertEquals(10, scale.update(0.125f));
    }
}
