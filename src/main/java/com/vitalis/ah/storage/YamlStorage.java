package com.vitalis.ah.storage;

import com.vitalis.ah.model.Listing;
import com.vitalis.ah.util.ItemSer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * YAML storage: plugins/AuctionHouse/listings.yml
 * Active listings + expired item storage per player. Thread-safe maps, saves async.
 */
public class YamlStorage {
    private final JavaPlugin plugin;
    private final File file;

    /** id -> listing */
    private final Map<UUID, Listing> active = new ConcurrentHashMap<>();
    /** player -> list of stored item Base64 (expired / cancelled) */
    private final Map<UUID, List<String>> vault = new ConcurrentHashMap<>();

    public YamlStorage(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    public synchronized void load() {
        active.clear();
        vault.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("active");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    ConfigurationSection s = sec.getConfigurationSection(key);
                    if (s == null) continue;
                    UUID id = UUID.fromString(key);
                    Listing l = new Listing(
                            id,
                            UUID.fromString(s.getString("seller")),
                            s.getString("sellerName", "?"),
                            s.getDouble("price"),
                            s.getLong("createdAt"),
                            s.getLong("expiresAt"),
                            s.getString("item"),
                            s.getString("material", "STONE"),
                            s.getString("displayName", ""),
                            s.getBoolean("enchanted", false));
                    active.put(id, l);
                } catch (Exception e) {
                    plugin.getLogger().warning("Skip broken listing " + key + ": " + e.getMessage());
                }
            }
        }
        ConfigurationSection vs = y.getConfigurationSection("vault");
        if (vs != null) {
            for (String key : vs.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    vault.put(uuid, new ArrayList<>(vs.getStringList(key)));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public synchronized void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Listing l : active.values()) {
            String p = "active." + l.id;
            y.set(p + ".seller", l.seller.toString());
            y.set(p + ".sellerName", l.sellerName);
            y.set(p + ".price", l.price);
            y.set(p + ".createdAt", l.createdAt);
            y.set(p + ".expiresAt", l.expiresAt);
            y.set(p + ".item", l.itemBase64);
            y.set(p + ".material", l.material);
            y.set(p + ".displayName", l.displayName);
            y.set(p + ".enchanted", l.enchanted);
        }
        for (Map.Entry<UUID, List<String>> e : vault.entrySet()) {
            y.set("vault." + e.getKey(), e.getValue());
        }
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Save failed: " + e.getMessage());
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
    }

    // ---- active ----
    public synchronized Collection<Listing> allActive() {
        return new ArrayList<>(active.values());
    }

    public synchronized Listing get(UUID id) {
        return active.get(id);
    }

    /** Atomic remove: returns listing only if present (anti-dupe on buy). */
    public synchronized Listing remove(UUID id) {
        return active.remove(id);
    }

    public synchronized void put(Listing l) {
        active.put(l.id, l);
    }

    public synchronized int countBy(UUID seller) {
        int n = 0;
        for (Listing l : active.values()) {
            if (l.seller.equals(seller)) n++;
        }
        return n;
    }

    // ---- expired vault ----
    public synchronized void stash(UUID player, ItemStack item) {
        vault.computeIfAbsent(player, k -> new ArrayList<>()).add(ItemSer.toBase64(item));
    }

    public synchronized void stashBase64(UUID player, String base64) {
        vault.computeIfAbsent(player, k -> new ArrayList<>()).add(base64);
    }

    public synchronized List<ItemStack> takeAll(UUID player) {
        List<String> list = vault.remove(player);
        if (list == null) return Collections.emptyList();
        List<ItemStack> out = new ArrayList<>();
        for (String s : list) {
            try {
                out.add(ItemSer.fromBase64(s));
            } catch (Exception e) {
                plugin.getLogger().warning("Skip broken stored item for " + player);
            }
        }
        return out;
    }

    public List<String> peekBase64(UUID player) {
        return vault.getOrDefault(player, Collections.emptyList());
    }

    public int vaultSize(UUID player) {
        List<String> l = vault.get(player);
        return l == null ? 0 : l.size();
    }
}
