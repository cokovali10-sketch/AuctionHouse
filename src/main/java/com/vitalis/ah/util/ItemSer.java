package com.vitalis.ah.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

/** ItemStack <-> Base64 (safe on Purpur/Paper 1.21). */
public final class ItemSer {
    private ItemSer() {}

    public static String toBase64(ItemStack item) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             BukkitObjectOutputStream data = new BukkitObjectOutputStream(out)) {
            data.writeObject(item);
            data.flush();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("serialize item failed", e);
        }
    }

    public static ItemStack fromBase64(String s) {
        byte[] bytes = Base64.getDecoder().decode(s);
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream data = new BukkitObjectInputStream(in)) {
            Object o = data.readObject();
            return (ItemStack) o;
        } catch (Exception e) {
            throw new IllegalStateException("deserialize item failed", e);
        }
    }
}
