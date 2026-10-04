package com.vitalis.ah.hook;

import com.vitalis.ah.AuctionHouse;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/** %ah_active% %ah_mine% %ah_expired% %ah_slots% — only if PlaceholderAPI installed. */
public class AhPlaceholders extends PlaceholderExpansion {
    private final AuctionHouse plugin;

    public AhPlaceholders(AuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "ah";
    }

    @Override
    public String getAuthor() {
        return "Vitalis";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (p == null) {
            if (params.equalsIgnoreCase("active")) {
                return String.valueOf(plugin.storage().allActive().size());
            }
            return "";
        }
        return switch (params.toLowerCase()) {
            case "active" -> String.valueOf(plugin.storage().allActive().size());
            case "mine" -> String.valueOf(plugin.storage().countBy(p.getUniqueId()));
            case "expired" -> String.valueOf(plugin.storage().vaultSize(p.getUniqueId()));
            case "slots" -> plugin.storage().countBy(p.getUniqueId())
                    + "/" + com.vitalis.ah.util.Slots.maxFor(p, plugin.cfg().maxPerPlayer);
            default -> null;
        };
    }
}
