package com.vitalis.ah;

import com.vitalis.ah.command.AhCommand;
import com.vitalis.ah.config.AhConfig;
import com.vitalis.ah.economy.EconomyBridge;
import com.vitalis.ah.gui.AhGui;
import com.vitalis.ah.listener.GuiListener;
import com.vitalis.ah.storage.YamlStorage;
import com.vitalis.ah.task.ExpireTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * AuctionHouse v1.0.0 - Paper/Purpur 1.21.11.
 * GUI auction: /ah, /ah sell, /ah search. Vault + Essentials economy.
 * Storage: plugins/AuctionHouse/listings.yml (no database needed).
 */
public class AuctionHouse extends JavaPlugin {
    private final AhConfig cfg = new AhConfig();
    private YamlStorage storage;
    private final EconomyBridge economy = new EconomyBridge(this);
    private AhGui gui;

    @Override
    public void onEnable() {
        cfg.load(this);
        storage = new YamlStorage(this, cfg.storageFile);
        storage.load();
        gui = new AhGui(cfg, storage);

        if (!economy.setup()) {
            getLogger().severe("No economy found (Vault or Essentials)! Auction needs money to work.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Economy hooked via " + economy.mode() + ".");
        gui.setEconomy(economy);

        AhCommand cmd = new AhCommand(this);
        if (getCommand("ah") != null) {
            getCommand("ah").setExecutor(cmd);
            getCommand("ah").setTabCompleter(cmd);
        }
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);

        long expireTicks = Math.max(1, cfg.expireCheckMinutes) * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, new ExpireTask(this), expireTicks, expireTicks);
        long saveTicks = Math.max(1, cfg.autosaveMinutes) * 60L * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> storage.save(), saveTicks, saveTicks);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                new com.vitalis.ah.hook.AhPlaceholders(this).register();
                getLogger().info("PlaceholderAPI hooked: %ah_active% %ah_mine% %ah_expired% %ah_slots%");
            } catch (Throwable t) {
                getLogger().warning("PlaceholderAPI hook failed: " + t.getMessage());
            }
        }

        getLogger().info("AuctionHouse enabled: tax=" + cfg.taxPercent + "%, slots=" + cfg.maxPerPlayer
                + ", duration=" + cfg.durationSeconds + "s, listings=" + storage.allActive().size()
                + ", exchange=" + (cfg.exchangeEnabled ? ("on (" + cfg.diamondPrice + cfg.symbol + "/diamond)")
                        : "off"));
        getLogger().info("AuctionHouse UI: AhTheme dark-fantasy/gold active (build 2).");
    }

    @Override
    public void onDisable() {
        if (storage != null) storage.save();
        getLogger().info("AuctionHouse disabled, data saved.");
    }

    public AhConfig cfg() {
        return cfg;
    }

    public YamlStorage storage() {
        return storage;
    }

    public EconomyBridge economy() {
        return economy;
    }

    public AhGui gui() {
        return gui;
    }

    /** /ah reload — re-reads config.yml without restart. */
    public void reloadPlugin() {
        cfg.load(this);
        getLogger().info("Config reloaded: tax=" + cfg.taxPercent + "%, exchange=" + cfg.exchangeEnabled
                + ", diamond=" + cfg.diamondPrice + ", symbol=" + cfg.symbol);
    }
}
