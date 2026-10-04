package com.vitalis.ah.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MsgTest {

    @Test
    void legacyCodesParse() {
        String plain = Msg.plain(Msg.color("&7Продавец: &fVitalis"));
        assertEquals("Продавец: Vitalis", plain);
        assertFalse(plain.contains("&7"));
    }

    @Test
    void minimessageGradientIsNotRenderedAsRawText() {
        String raw = Msg.plain(Msg.color("<gradient:#FFD700:#FFA500>◆ 1,000 монет ◆</gradient>"));
        assertEquals("◆ 1,000 монет ◆", raw);
        assertFalse(raw.contains("<gradient"), "gradient tag leaked into visible text");
        assertFalse(raw.contains("</gradient>"), "gradient tag leaked into visible text");
    }

    @Test
    void mixedLegacyAndMinimessageInOneString() {
        String raw = Msg.plain(Msg.color("&eБаланс: <white>100</white>"));
        assertEquals("Баланс: 100", raw);
        assertFalse(raw.contains("&e"));
        assertFalse(raw.contains("<white>"));
    }

    @Test
    void guiButtonStyleStringParses() {
        // exact string used for the exchanger button in AhGui
        String raw = Msg.plain(Msg.color(
                "<gradient:#FFD700:#FFA500>◆ Обменник ◆</gradient>"));
        assertEquals("◆ Обменник ◆", raw);
    }

    @Test
    void brokenTagNeverThrows() {
        assertDoesNotThrow(() -> Msg.color("<gradient:oops"));
        assertDoesNotThrow(() -> Msg.color("3 < 5 and 7 > 4 &a ok"));
        assertDoesNotThrow(() -> Msg.color(null));
        assertDoesNotThrow(() -> Msg.color(""));
    }

    @Test
    void nullAndEmptyAreSafe() {
        assertEquals("", Msg.plain(Msg.color(null)));
        assertEquals("", Msg.plain(Msg.color("")));
        assertEquals("", Msg.plain(null));
    }

    @Test
    void legacyToMiniConvertsCodes() {
        assertEquals("<gray>hi", Msg.legacyToMini("&7hi"));
        assertEquals("<#AABBCC>hex", Msg.legacyToMini("&#AABBCC" + "hex"));
        assertEquals("plain & untouched", Msg.legacyToMini("plain & untouched"));
        assertEquals("<reset><bold>x", Msg.legacyToMini("&r&lx"));
    }
}
