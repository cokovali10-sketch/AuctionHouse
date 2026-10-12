package com.vitalis.ah.gui;

import com.vitalis.ah.config.AhConfig;
import com.vitalis.ah.util.Fmt;
import com.vitalis.ah.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Single source of the auction's dark-fantasy/gold look.
 * Only presentation lives here: titles, buttons, filler, rarity badges,
 * empty states. Prices, taxes, slots and click behavior stay in AhGui.
 */
public final class AhTheme {
    // palette
    public static final String GOLD = "<gradient:#FFD700:#FFA500>";
    public static final String GOLD_END = "</gradient>";
    public static final String INFO = "<gradient:#7AD9FF:#FFD76A>";
    public static final String DANGER = "<gradient:#FF6B6B:#FF8E8E>";
    public static final String GO = "<gradient:#7DFF9A:#7AD9FF>";

    public enum Rarity { COMMON, RARE, EPIC }

    private AhTheme() {
    }

    public static String titleBrowser(int page, int pages, int count) {
        return "<gradient:#FFD700:#FFA500>◆ Аукцион ◆</gradient> <gray>" + page + "/" + pages
                + " <dark_gray>•</dark_gray> <white>" + count + "</white></gray>";
    }

    public static String titleMy(int count, int max) {
        return "<gradient:#FFD700:#FFA500>◆ Мои лоты ◆</gradient> <gray><white>" + count
                + "</white>/" + max + "</gray>";
    }

    public static String titleSearch(String query, int page, int pages) {
        return "<gradient:#FFD76A:#7AD9FF>Поиск</gradient> <gray>" + query
                + " <gray>" + page + "/" + pages + "</gray>";
    }

    public static Rarity rarity(double price, double rareAt, double epicAt) {
        if (price >= epicAt) {
            return Rarity.EPIC;
        }
        if (price >= rareAt) {
            return Rarity.RARE;
        }
        return Rarity.COMMON;
    }

    public static String rarityLabel(Rarity r, AhConfig cfg) {
        return switch (r) {
            case EPIC -> "<gradient:#FF6B6B:#FFD700>✦ ЛЕГЕНДАРНЫЙ ЛОТ ✦</gradient>";
            case RARE -> "<gradient:#7AD9FF:#FFD76A>◆ Редкий лот ◆</gradient>";
            default -> "<gray>Обычный лот</gray>";
        };
    }

    /** Rarity badge lore line, null for common (keeps common lore short). */
    public static Component rarityLine(Rarity r, AhConfig cfg) {
        if (r == Rarity.COMMON) {
            return null;
        }
        return Msg.color(rarityLabel(r, cfg));
    }

    public static ItemStack filler(AhConfig cfg) {
        ItemStack it = new ItemStack(cfg.filler);
        ItemMeta m = it.getItemMeta();
        m.displayName(Msg.color("<gradient:#14141f:#0f0f1a> </gradient>"));
        it.setItemMeta(m);
        return it;
    }

    public static ItemStack navBack(int page, int pages) {
        return button(Material.RED_STAINED_GLASS_PANE,
                DANGER + "← Назад" + GOLD_END,
                "<gray>Страница " + page + "/" + pages + "</gray>");
    }

    public static ItemStack navForward(int page, int pages) {
        return button(Material.LIME_STAINED_GLASS_PANE,
                GO + "▸ Вперёд ▸" + GOLD_END,
                "<gray>Страница <white>" + page + "</white><gray>/</gray><white>" + pages + "</white></gray>");
    }

    public static ItemStack closeButton(String balance) {
        return button(Material.BARRIER, DANGER + "◆ Закрыть ◆" + GOLD_END,
                "<gray>Баланс: <gold>" + balance + "</gold></gray>");
    }

    public static ItemStack backButton() {
        return button(Material.ARROW, "&7Назад");
    }

    public static ItemStack emptyBrowser() {
        return button(Material.CHEST,
                GOLD + "◆ Пока пусто ◆" + GOLD_END,
                "<gray>Лотов нет — стань первым!</gray>",
                "<gray>Выставь предмет: <white>/ah sell <цена></white></gray>");
    }

    public static ItemStack emptySearch(String query) {
        return button(Material.COMPASS,
                INFO + "◆ Ничего не найдено ◆" + GOLD_END,
                "<gray>Запрос: <white>" + query + "</white></gray>",
                "<gray>Попробуй другое слово</gray>");
    }

    public static ItemStack emptyVault() {
        return button(Material.ENDER_CHEST,
                GOLD + "◆ Склад пуст ◆" + GOLD_END,
                "<gray>Снятые и просроченные лоты появятся здесь</gray>");
    }

    public static ItemStack button(Material mat, String name, String... loreLines) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Msg.color(name));
        if (loreLines != null && loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String s : loreLines) {
                if (s != null) {
                    lore.add(Msg.color(s));
                }
            }
            m.lore(lore);
        }
        it.setItemMeta(m);
        return it;
    }

    public static String divider() {
        return INFO + "────────────────" + GOLD_END;
    }

    public static String priceLine(double price, String symbol) {
        return GOLD + "◆ " + Fmt.money(price, symbol) + " монет ◆" + GOLD_END;
    }
}
