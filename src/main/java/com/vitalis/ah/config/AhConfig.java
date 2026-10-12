package com.vitalis.ah.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Typed wrapper over config.yml (Russian comments inside file). */
public class AhConfig {
    public String prefix;
    public int expireCheckMinutes;
    public boolean soldMessage;
    public double announceMinPrice;

    public double taxPercent;
    public double minPrice;
    public double maxPrice;
    public String symbol;

    public int maxPerPlayer;
    public long durationSeconds;
    public boolean allowDamaged;
    public Set<Material> blocked = new HashSet<>();

    public String titleMain;
    public String titleConfirm;
    public String titleMyLots;
    public String titleExpired;
    public String defaultSort;
    public Material filler;

    public String storageFile;
    public int autosaveMinutes;

    public boolean sounds;
    public String defaultCategory;
    public java.util.Set<String> blockedWorlds = new java.util.HashSet<>();
    public boolean exchangeEnabled;
    public double diamondPrice;
    public double rareAt;
    public double epicAt;

    public void load(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        prefix = c.getString("general.prefix", "&8[&6Auction&eHouse&8] ");
        expireCheckMinutes = c.getInt("general.expire-check-minutes", 5);
        soldMessage = c.getBoolean("general.sold-message", true);
        announceMinPrice = c.getDouble("general.announce-min-price", 0.0);

        taxPercent = c.getDouble("economy.tax-percent", 10.0);
        // min/max/symbol live under `economy:` in config.yml, older files kept them
        // under `exchange:` — accept both so upgrades don't silently fall back.
        minPrice = optDouble(c, "economy.min-price", "exchange.min-price", 1.0);
        maxPrice = optDouble(c, "economy.max-price", "exchange.max-price", 100000000.0);
        symbol = c.contains("economy.symbol")
                ? c.getString("economy.symbol", "$")
                : c.getString("exchange.symbol", "$");

        maxPerPlayer = c.getInt("listings.max-per-player", 100);
        durationSeconds = c.getLong("listings.duration-seconds", 604800L);
        allowDamaged = c.getBoolean("listings.allow-damaged", true);
        blocked.clear();
        List<String> bl = c.getStringList("listings.blocked");
        for (String s : bl) {
            try {
                blocked.add(Material.valueOf(s.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        titleMain = c.getString("gui.title-main", "&8Auction &7- page &e{page}");
        titleConfirm = c.getString("gui.title-confirm", "&8Confirm purchase");
        titleMyLots = c.getString("gui.title-mylots", "&8My listings &7({count}/{max})");
        titleExpired = c.getString("gui.title-expired", "&8Expired / Storage");
        defaultSort = c.getString("gui.default-sort", "NEWEST");
        try {
            filler = Material.valueOf(c.getString("gui.filler", "GRAY_STAINED_GLASS_PANE").toUpperCase());
        } catch (IllegalArgumentException e) {
            filler = Material.GRAY_STAINED_GLASS_PANE;
        }

        storageFile = c.getString("storage.file", "listings.yml");
        autosaveMinutes = c.getInt("storage.autosave-minutes", 10);

        sounds = c.getBoolean("gui.sounds", true);
        defaultCategory = c.getString("gui.default-category", "ALL");
        exchangeEnabled = c.getBoolean("exchange.enabled", true);
        diamondPrice = c.getDouble("exchange.diamond-price", 1000.0);
        rareAt = c.getDouble("style.rare-at", 10000.0);
        epicAt = c.getDouble("style.epic-at", 100000.0);
        blockedWorlds.clear();
        for (String w : c.getStringList("listings.blocked-worlds")) {
            blockedWorlds.add(w.toLowerCase(java.util.Locale.ROOT));
        }
    }

    private static double optDouble(FileConfiguration c, String preferred, String legacyPath, double def) {
        if (c.contains(preferred)) return c.getDouble(preferred);
        if (c.contains(legacyPath)) return c.getDouble(legacyPath);
        return def;
    }
}
