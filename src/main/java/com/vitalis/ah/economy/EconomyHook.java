package com.vitalis.ah.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/** Vault bridge (works with EssentialsX economy). */
public class EconomyHook {
    private final JavaPlugin plugin;
    private Economy economy;

    public EconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy get() {
        return economy;
    }

    public boolean has(OfflinePlayer p, double amount) {
        return economy.has(p, amount);
    }

    public boolean withdraw(OfflinePlayer p, double amount) {
        return economy.withdrawPlayer(p, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer p, double amount) {
        return economy.depositPlayer(p, amount).transactionSuccess();
    }

    public String format(double amount) {
        try {
            return economy.format(amount);
        } catch (Exception e) {
            return String.valueOf(amount);
        }
    }
}
