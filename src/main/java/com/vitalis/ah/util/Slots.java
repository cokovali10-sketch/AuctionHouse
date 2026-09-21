package com.vitalis.ah.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 * Per-rank listing limits via permission auctionhouse.slots.<N>.
 * Works with LuckPerms/any permission plugin. Falls back to config default.
 * Example: VIP group gets auctionhouse.slots.200, default players get config (100).
 */
public final class Slots {
    private static final String PREFIX = "auctionhouse.slots.";

    private Slots() {}

    public static int maxFor(Player p, int def) {
        int best = def;
        try {
            for (PermissionAttachmentInfo info : p.getEffectivePermissions()) {
                String name = info.getPermission().toLowerCase(java.util.Locale.ROOT);
                if (!name.startsWith(PREFIX)) continue;
                if (!info.getValue()) continue; // negated perm, skip
                try {
                    int n = Integer.parseInt(name.substring(PREFIX.length()));
                    if (n > best) best = n;
                } catch (NumberFormatException ignored) {}
            }
        } catch (Exception ignored) {}
        return best;
    }
}
