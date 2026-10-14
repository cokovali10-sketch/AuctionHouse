package com.vitalis.ah.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyFormatTest {

    @Test
    void wholeNumbersOnly() {
        assertEquals("12,313$", Fmt.money(12313.1, "$"));
        assertEquals("12,313$", Fmt.money(12313.0, "$"));
        assertEquals("1,000$", Fmt.money(1000.0, "$"));
        assertEquals("0$", Fmt.money(0.0, "$"));
    }

    @Test
    void roundsHalfUp() {
        assertEquals("1,001$", Fmt.money(1000.5, "$"));
        assertEquals("1,000$", Fmt.money(1000.4, "$"));
    }
}
