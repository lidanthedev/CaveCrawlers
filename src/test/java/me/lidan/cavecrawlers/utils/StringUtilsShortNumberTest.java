package me.lidan.cavecrawlers.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilsShortNumberTest {

    @Test
    void roundsSmallNumbersToTwoDecimals() {
        assertEquals("498.3", StringUtils.getShortNumber(498.3000000000012));
        assertEquals("660", StringUtils.getShortNumber(660));
        assertEquals("0.33", StringUtils.getShortNumber(1 / 3d));
        assertEquals("-5.5", StringUtils.getShortNumber(-5.5));
    }

    @Test
    void keepsInnerZeroDecimals() {
        assertEquals("1.05k", StringUtils.getShortNumber(1050));
        assertEquals("1k", StringUtils.getShortNumber(1000));
        assertEquals("2.5m", StringUtils.getShortNumber(2_500_000));
    }
}
