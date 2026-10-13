package com.vitalis.ah.gui;

import com.vitalis.ah.util.Msg;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Theme unit tests: tiers, titles, MiniMessage safety. No server needed. */
class AhThemeTest {
    @Test
    void rarityTiers() {
        assertEquals(AhTheme.Rarity.COMMON, AhTheme.rarity(9.99, 10000, 100000));
        assertEquals(AhTheme.Rarity.COMMON, AhTheme.rarity(0, 10000, 100000));
        assertEquals(AhTheme.Rarity.RARE, AhTheme.rarity(10000, 10000, 100000));
        assertEquals(AhTheme.Rarity.RARE, AhTheme.rarity(99999.99, 10000, 100000));
        assertEquals(AhTheme.Rarity.EPIC, AhTheme.rarity(100000, 10000, 100000));
        assertEquals(AhTheme.Rarity.EPIC, AhTheme.rarity(1e9, 10000, 100000));
    }

    @Test
    void everyTierHasBadge() {
        // Every listing shows a badge so all lores visibly differ.
        assertNotNull(AhTheme.rarityLine(AhTheme.Rarity.COMMON, null));
        assertNotNull(AhTheme.rarityLine(AhTheme.Rarity.RARE, null));
        assertNotNull(AhTheme.rarityLine(AhTheme.Rarity.EPIC, null));
    }

    @Test
    void titlesContainNumbers() {
        assertTrue(Msg.plain(Msg.color(AhTheme.titleBrowser(2, 5, 100))).contains("2/5"));
        assertTrue(Msg.plain(Msg.color(AhTheme.titleMy(3, 100))).contains("3"));
        assertTrue(Msg.plain(Msg.color(AhTheme.titleSearch("алмаз", 1, 2))).contains("алмаз"));
    }

    @Test
    void gradientsParse() {
        // Must not throw: every themed string is valid MiniMessage.
        Component c1 = Msg.color(AhTheme.priceLine(1500, "$"));
        Component c2 = Msg.color(AhTheme.divider());
        assertFalse(Msg.plain(c1).isEmpty());
        assertFalse(Msg.plain(c2).isEmpty());
    }
}
