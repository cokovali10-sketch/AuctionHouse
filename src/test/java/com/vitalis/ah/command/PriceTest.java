package com.vitalis.ah.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PriceTest {

    @Test
    void validPrices() {
        assertEquals(1500.0, AhCommand.parsePrice("1500"));
        assertEquals(15.5, AhCommand.parsePrice("15.5"));
        assertEquals(15.5, AhCommand.parsePrice("15,5"));
        assertEquals(1.0, AhCommand.parsePrice("1"));
    }

    @Test
    void nanAndInfinityRejected() {
        // Double.parseDouble accepts "NaN"/"Infinity": they used to pass the
        // min/max range check (NaN comparisons are false) and poison economy
        assertNull(AhCommand.parsePrice("NaN"));
        assertNull(AhCommand.parsePrice("Infinity"));
        assertNull(AhCommand.parsePrice("-Infinity"));
        assertNull(AhCommand.parsePrice("1e400"));
    }

    @Test
    void garbageRejected() {
        assertNull(AhCommand.parsePrice("abc"));
        assertNull(AhCommand.parsePrice(""));
        assertNull(AhCommand.parsePrice(null));
        assertNull(AhCommand.parsePrice("-"));
    }
}
