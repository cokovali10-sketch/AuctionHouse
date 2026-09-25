package com.vitalis.ah.gui;

import com.vitalis.ah.config.AhConfig;
import com.vitalis.ah.economy.EconomyBridge;
import com.vitalis.ah.model.Listing;
import com.vitalis.ah.storage.YamlStorage;
import com.vitalis.ah.util.Fmt;
import com.vitalis.ah.util.ItemSer;
import com.vitalis.ah.util.Msg;
import com.vitalis.ah.util.Slots;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/** All AuctionHouse GUIs: browser (double chest), categories, confirm, expired storage. */
public class AhGui {

    public enum Sort { NEWEST, PRICE_ASC, PRICE_DESC, TIME_LEFT }

    public enum Mode { ALL, MY, SEARCH }

    public static class State {
        public Mode mode = Mode.ALL;
        public int page = 0;
        public String query = "";
        public Sort sort = Sort.NEWEST;
        public Category category = Category.ALL;
    }

    public static final int PAGE_SIZE = 45;

    private final AhConfig cfg;
    private final YamlStorage storage;
    private EconomyBridge economy;

    /** viewer -> current browser state */
    private final Map<UUID, State> states = new HashMap<>();
    /** viewer -> listing id awaiting confirm */
    private final Map<UUID, UUID> pendingConfirm = new HashMap<>();
    /** viewer -> expired GUI slot -> vault index */
    private final Map<UUID, List<Integer>> expiredSlotMap = new HashMap<>();
    /** viewers waiting for chat search input */
    private final Set<UUID> pendingSearch = new HashSet<>();

    public AhGui(AhConfig cfg, YamlStorage storage) {
        this.cfg = cfg;
        this.storage = storage;
    }

    /** Called by main after Vault hooks up (economy may be null in GUI before that). */
    public void setEconomy(EconomyBridge economy) {
        this.economy = economy;
    }

    private String balanceOf(Player p) {
        if (economy == null || !economy.ready()) return "?";
        try {
            return Fmt.money(economy.balance(p), cfg.symbol);
        } catch (Exception e) {
            return "?";
        }
    }

    public State stateOf(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> {
            State s = new State();
            s.sort = parseSort(cfg.defaultSort);
            s.category = Category.parse(cfg.defaultCategory);
            return s;
        });
    }

    public static Sort parseSort(String s) {
        try {
            return Sort.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return Sort.NEWEST;
        }
    }

    public void markConfirm(Player p, UUID listingId) {
        pendingConfirm.put(p.getUniqueId(), listingId);
    }

    public UUID takeConfirm(Player p) {
        return pendingConfirm.remove(p.getUniqueId());
    }

    public void markSearchInput(Player p) {
        pendingSearch.add(p.getUniqueId());
    }

    public boolean takeSearchInput(Player p) {
        return pendingSearch.remove(p.getUniqueId());
    }

    public void cancelSearchInput(Player p) {
        pendingSearch.remove(p.getUniqueId());
    }

    // ---------------- browser ----------------

    public List<Listing> view(State st, UUID viewer) {
        List<Listing> all = new ArrayList<>(storage.allActive());
        long now = System.currentTimeMillis();
        all.removeIf(l -> l.expired(now));
        if (st.mode == Mode.MY) {
            all.removeIf(l -> !l.seller.equals(viewer));
        } else if (st.mode == Mode.SEARCH && !st.query.isEmpty()) {
            String q = st.query.toLowerCase(Locale.ROOT);
            all.removeIf(l -> !matches(l, q));
        }
        if (st.category != Category.ALL) {
            all.removeIf(l -> !st.category.matches(safeMaterial(l.material), l.enchanted));
        }
        Comparator<Listing> cmp;
        switch (st.sort) {
            case PRICE_ASC -> cmp = Comparator.comparingDouble(l -> l.price);
            case PRICE_DESC -> cmp = Comparator.comparingDouble((Listing l) -> l.price).reversed();
            case TIME_LEFT -> cmp = Comparator.comparingLong(l -> l.expiresAt);
            default -> cmp = Comparator.comparingLong((Listing l) -> l.createdAt).reversed();
        }
        all.sort(cmp);
        return all;
    }

    private Material safeMaterial(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

    private boolean matches(Listing l, String q) {
        if (l.material.toLowerCase(Locale.ROOT).contains(q)) return true;
        if (l.displayName.toLowerCase(Locale.ROOT).contains(q)) return true;
        if (l.sellerName.toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    public void openBrowser(Player p, State st) {
        states.put(p.getUniqueId(), st);
        List<Listing> list = view(st, p.getUniqueId());
        int pages = Math.max(1, (int) Math.ceil(list.size() / (double) PAGE_SIZE));
        st.page = Math.max(0, Math.min(st.page, pages - 1));

        int maxSlots = Slots.maxFor(p, cfg.maxPerPlayer);
        String title = cfg.titleMain.replace("{page}", (st.page + 1) + "/" + pages);
        if (st.mode == Mode.MY) title = cfg.titleMyLots
                .replace("{count}", String.valueOf(countMine(p)))
                .replace("{max}", String.valueOf(maxSlots));
        if (st.mode == Mode.SEARCH) title = "&8Поиск: &e" + st.query + " &7- &e" + (st.page + 1) + "/" + pages;

        Inventory inv = Bukkit.createInventory(new BrowserHolder(), 54, Msg.color(title));

        int from = st.page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, list.size());
        for (int i = from; i < to; i++) {
            inv.setItem(i - from, renderListing(list.get(i), p.getUniqueId()));
        }
        // nav row
        ItemStack fill = filler();
        for (int s = 45; s < 54; s++) inv.setItem(s, fill);

        inv.setItem(45, button(Material.RED_STAINED_GLASS_PANE, "&c← Назад", "&7Страница " + (st.page + 1) + "/" + pages));
        inv.setItem(46, button(Material.BOOK, "&bМои лоты",
                "&7Ваши: &e" + countMine(p) + "&7/&e" + maxSlots,
                "&7Баланс: &e" + balanceOf(p), "&7Клик — открыть"));
        inv.setItem(47, button(Material.HOPPER, "&6Просрочка / Склад",
                "&7Забрать: &e" + storage.vaultSize(p.getUniqueId()), "&7Клик — открыть"));
        inv.setItem(48, button(Material.COMPARATOR, "&dСорт: &e" + sortRu(st.sort),
                "&7Клик — сменить"));
        inv.setItem(49, button(Material.BARRIER, "&cЗакрыть"));
        inv.setItem(50, button(st.category.icon, "&aКатегория: &e" + st.category.nameRu,
                "&7Клик — выбрать"));
        inv.setItem(51, button(Material.COMPASS, "&aПоиск",
                st.query.isEmpty() ? "&7Клик — ввести запрос" : "&7Запрос: &e" + st.query,
                "&7Команда: &e/ah search <текст>"));
        inv.setItem(52, button(Material.SUNFLOWER, "&eОбновить", "&7Клик — обновить список"));
        inv.setItem(53, button(Material.LIME_STAINED_GLASS_PANE, "&aВперёд →", "&7Страница " + (st.page + 1) + "/" + pages));
        p.openInventory(inv);
    }

    private String sortRu(Sort s) {
        return switch (s) {
            case NEWEST -> "Новые";
            case PRICE_ASC -> "Дешевле";
            case PRICE_DESC -> "Дороже";
            case TIME_LEFT -> "Скоро конец";
        };
    }

    private int countMine(Player p) {
        return storage.countBy(p.getUniqueId());
    }

    private ItemStack renderListing(Listing l, UUID viewer) {
        ItemStack base;
        try {
            base = ItemSer.fromBase64(l.itemBase64).clone();
        } catch (Exception e) {
            base = new ItemStack(Material.STONE);
        }
        ItemMeta meta = base.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Msg.color("&6Цена: &e" + Fmt.money(l.price, cfg.symbol)));
        lore.add(Msg.color("&7Продавец: &f" + l.sellerName));
        lore.add(Msg.color("&7До конца: &e" + Fmt.timeLeft(l.expiresAt - System.currentTimeMillis())));
        if (l.seller.equals(viewer)) {
            lore.add(Msg.color("&eВаш лот — клик = снять"));
        } else {
            lore.add(Msg.color("&aКлик — купить"));
        }
        meta.lore(lore);
        base.setItemMeta(meta);
        return base;
    }

    // ---------------- categories ----------------

    public void openCategories(Player p) {
        Inventory inv = Bukkit.createInventory(new CategoriesHolder(), 27, Msg.color("&8Категория"));
        ItemStack fill = filler();
        for (int s = 0; s < 27; s++) inv.setItem(s, fill);
        State st = stateOf(p);
        Category[] cats = Category.values();
        int slot = 9;
        for (Category c : cats) {
            boolean selected = st.category == c;
            inv.setItem(slot++, button(c.icon,
                    (selected ? "&a▶ " : "&7") + c.nameRu,
                    selected ? "&aВыбрано" : "&7Клик — выбрать"));
        }
        inv.setItem(22, button(Material.BARRIER, "&cНазад"));
        p.openInventory(inv);
    }

    // ---------------- confirm ----------------

    public void openConfirm(Player p, Listing l) {
        markConfirm(p, l.id);
        Inventory inv = Bukkit.createInventory(new ConfirmHolder(), 27, Msg.color(cfg.titleConfirm));
        ItemStack fill = filler();
        for (int s = 0; s < 27; s++) inv.setItem(s, fill);
        inv.setItem(13, renderListing(l, p.getUniqueId()));
        double tax = l.price * cfg.taxPercent / 100.0;
        inv.setItem(11, button(Material.GREEN_WOOL, "&aКУПИТЬ за " + Fmt.money(l.price, cfg.symbol),
                "&7Продавец получит &e" + Fmt.money(l.price - tax, cfg.symbol),
                "&7Комиссия " + cfg.taxPercent + "%",
                "&7Товар: &f" + l.displayName));
        inv.setItem(15, button(Material.RED_WOOL, "&cОтмена"));
        p.openInventory(inv);
    }

    // ---------------- expired ----------------

    public void openExpired(Player p) {
        List<String> items = storage.peekBase64(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(new ExpiredHolder(), 54, Msg.color(cfg.titleExpired));
        List<Integer> map = new ArrayList<>();
        int slot = 0;
        for (String b64 : items) {
            if (slot >= 45) break;
            try {
                inv.setItem(slot, ItemSer.fromBase64(b64));
                map.add(slot);
            } catch (Exception ignored) {}
            slot++;
        }
        ItemStack fill = filler();
        for (int s = 45; s < 54; s++) inv.setItem(s, fill);
        inv.setItem(49, button(Material.BARRIER, "&cНазад"));
        inv.setItem(53, button(Material.CHEST, "&eЗабрать ВСЁ (" + items.size() + ")", "&7Клик — забрать всё"));
        expiredSlotMap.put(p.getUniqueId(), map);
        p.openInventory(inv);
    }

    public List<Integer> expiredMap(Player p) {
        return expiredSlotMap.getOrDefault(p.getUniqueId(), Collections.emptyList());
    }

    // ---------------- helpers ----------------

    private ItemStack filler() {
        ItemStack it = new ItemStack(cfg.filler);
        ItemMeta m = it.getItemMeta();
        m.displayName(Msg.color("&8 "));
        it.setItemMeta(m);
        return it;
    }

    private ItemStack button(Material mat, String name) {
        return button(mat, name, new String[0]);
    }

    private ItemStack button(Material mat, String name, String... loreLines) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Msg.color(name));
        if (loreLines != null && loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String s : loreLines) {
                if (s != null) lore.add(Msg.color(s));
            }
            m.lore(lore);
        }
        it.setItemMeta(m);
        return it;
    }

    // holders to identify our GUIs
    public static class BrowserHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public static class CategoriesHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public static class ConfirmHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public static class ExpiredHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    /** Pretty item name for search cache + messages. */
    public static String prettyName(ItemStack item) {
        if (item == null) return "?";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return Msg.plain(meta.displayName());
        }
        String m = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return m;
    }
}
