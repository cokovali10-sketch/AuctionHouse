package com.vitalis.ah.model;

import java.util.UUID;

/** One active listing on the auction. */
public class Listing {
    public final UUID id;
    public final UUID seller;
    public final String sellerName;
    public final double price;
    public final long createdAt;
    public final long expiresAt;
    /** Base64 ItemStack (single item/entry as listed, may be a stack). */
    public final String itemBase64;
    /** Cached material name for fast search without deserializing. */
    public final String material;
    /** Cached display name (plain) for search. */
    public final String displayName;
    /** True if item has any enchantments (for ENCHANTED category). */
    public final boolean enchanted;

    public Listing(UUID id, UUID seller, String sellerName, double price,
                   long createdAt, long expiresAt, String itemBase64,
                   String material, String displayName, boolean enchanted) {
        this.id = id;
        this.seller = seller;
        this.sellerName = sellerName;
        this.price = price;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.itemBase64 = itemBase64;
        this.material = material;
        this.displayName = displayName;
        this.enchanted = enchanted;
    }

    public boolean expired(long now) {
        return now >= expiresAt;
    }
}
