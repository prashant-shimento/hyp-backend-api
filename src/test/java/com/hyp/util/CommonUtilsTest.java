package com.hyp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommonUtilsTest {

    @Test
    @DisplayName("Should convert integer rupees to paise")
    void testIntegerAmount() {
        assertEquals("1000", CommonUtils.getISOAmount(10.0));
        assertEquals("500", CommonUtils.getISOAmount(5.0));
    }

    @Test
    @DisplayName("Should handle single decimal correctly")
    void testSingleDecimal() {
        assertEquals("1050", CommonUtils.getISOAmount(10.5));
        assertEquals("999", CommonUtils.getISOAmount(9.99));
    }

    @Test
    @DisplayName("Should handle two decimal places correctly")
    void testTwoDecimals() {
        assertEquals("1023", CommonUtils.getISOAmount(10.23));
        assertEquals("1999", CommonUtils.getISOAmount(19.99));
    }

    @Test
    @DisplayName("Should round properly when more than two decimals")
    void testRounding() {
        assertEquals("1057", CommonUtils.getISOAmount(10.567)); // 10.567 → 1056.7 → 1057
        assertEquals("1001", CommonUtils.getISOAmount(10.005)); // HALF_UP
    }

    @Test
    @DisplayName("Should handle zero correctly")
    void testZero() {
        assertEquals("0", CommonUtils.getISOAmount(0.0));
    }

    @Test
    @DisplayName("Should handle large amounts")
    void testLargeAmount() {
        assertEquals("12345678900", CommonUtils.getISOAmount(123456789.00));
    }

    @Test
    @DisplayName("Should handle floating precision edge case safely")
    void testFloatingPointEdgeCase() {
        assertEquals("1023", CommonUtils.getISOAmount(10.23));
    }
}
