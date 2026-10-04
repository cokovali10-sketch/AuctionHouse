package com.vitalis.ah.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExchangeTest {

    @Test
    void amountCycleWrapsAround() {
        int a = AhGui.nextExchangeAmount(64);
        assertEquals(-1, a, "64 -> ВСЁ");
        assertEquals(1, AhGui.nextExchangeAmount(-1), "ВСЁ -> 1");
        assertEquals(16, AhGui.nextExchangeAmount(1));
        assertEquals(32, AhGui.nextExchangeAmount(16));
        assertEquals(64, AhGui.nextExchangeAmount(32));
    }

    @Test
    void unknownAmountFallsBackTo64() {
        assertEquals(64, AhGui.nextExchangeAmount(7));
        assertEquals(64, AhGui.nextExchangeAmount(-5));
    }

    @Test
    void labels() {
        assertEquals("ВСЁ", AhGui.exchangeAmountLabel(-1));
        assertEquals("64", AhGui.exchangeAmountLabel(64));
        assertEquals("1", AhGui.exchangeAmountLabel(1));
    }

    @Test
    void allPresetsAreListed() {
        assertArrayEquals(new int[]{1, 16, 32, 64, -1}, AhGui.EXCHANGE_AMOUNTS);
    }
}
