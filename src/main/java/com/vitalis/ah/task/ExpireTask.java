package com.vitalis.ah.task;

import com.vitalis.ah.AuctionHouse;
import com.vitalis.ah.model.Listing;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Moves expired listings to sellers' storage + notifies online sellers. */
public class ExpireTask implements Runnable {
    private final AuctionHouse plugin;

    public ExpireTask(AuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        List<Listing> expired = new ArrayList<>();
        for (Listing l : plugin.storage().allActive()) {
            if (l.expired(now)) expired.add(l);
        }
        if (expired.isEmpty()) return;
        for (Listing l : expired) {
            Listing removed = plugin.storage().remove(l.id);
            if (removed == null) continue;
            plugin.storage().stashBase64(removed.seller, removed.itemBase64);
            Player seller = Bukkit.getPlayer(removed.seller);
            if (seller != null && seller.isOnline()) {
                seller.sendMessage(com.vitalis.ah.util.Msg.color(plugin.cfg().prefix
                        + "&eЛот истёк: &f" + removed.displayName + " &7→ забери на «Складе» в /ah"));
            }
        }
        plugin.storage().saveAsync();
        plugin.getLogger().info("Expired " + expired.size() + " listings -> storage.");
    }
}
