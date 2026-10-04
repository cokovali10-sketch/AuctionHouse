package com.vitalis.ah.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Text helpers. Supports three styles in ONE call:
 *  - legacy '&amp;7' codes
 *  - MiniMessage tags '&lt;gradient:...&gt; &lt;gray&gt; &lt;#AABBCC&gt;'
 *  - mixed strings, e.g. "&amp;eБаланс: &lt;white&gt;100&lt;/white&gt;"
 * Before MiniMessage tags were parsed this rendered as raw text in every GUI title/lore.
 */
public final class Msg {
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.builder().character('&').hexColors().build();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** MiniMessage-like tag: &lt;gradient:...&gt;, &lt;gray&gt;, &lt;#AABBCC&gt;, &lt;/reset&gt;. */
    private static final Pattern MINI_TAG = Pattern.compile("</?[a-zA-Z#][^<>]*>");
    /** Legacy codes: &amp;RRGGBB or &amp;[0-9a-fk-or] (both cases). */
    private static final Pattern LEGACY_CODE =
            Pattern.compile("&#([0-9A-Fa-f]{6})|&([0-9A-Fa-fK-Ok-oRr])");

    private Msg() {}

    public static Component color(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        if (MINI_TAG.matcher(text).find()) {
            try {
                return MINI.deserialize(legacyToMini(text));
            } catch (RuntimeException e) {
                // broken tag sequence -> strip tags so players never see raw markup
                return LEGACY.deserialize(MINI_TAG.matcher(legacyToMini(text)).replaceAll(""));
            }
        }
        return LEGACY.deserialize(text);
    }

    /** Turns &amp;7 / &amp;#AABBCC into MiniMessage tags so both styles can live in one string. */
    static String legacyToMini(String text) {
        Matcher m = LEGACY_CODE.matcher(text);
        if (!m.find()) return text;
        StringBuilder sb = new StringBuilder(text.length() + 16);
        int last = 0;
        do {
            sb.append(text, last, m.start());
            if (m.group(1) != null) {
                sb.append("<#").append(m.group(1)).append('>');
            } else {
                sb.append(tagFor(m.group(2).charAt(0)));
            }
            last = m.end();
        } while (m.find());
        sb.append(text.substring(last));
        return sb.toString();
    }

    private static String tagFor(char c) {
        return switch (Character.toLowerCase(c)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            case '#' -> "<white>";
            default -> "<white>";
        };
    }

    public static String strip(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', text));
    }

    /** Plain text of an Adventure component (for search). */
    public static String plain(Component c) {
        if (c == null) return "";
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
    }
}
