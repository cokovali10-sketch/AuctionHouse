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
import org.bukkit.Sound;

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
        /** exchanger amount preset */
        public int exchangeAmount = 64;
    }

    /** Amount presets for the exchanger, -1 = ALL. */
    public static final int[] EXCHANGE_AMOUNTS = {1, 16, 32, 64, -1};

    public static String exchangeAmountLabel(int amount) {
        return amount < 0 ? "ВСЁ" : String.valueOf(amount);
    }

    public static int nextExchangeAmount(int current) {
        for (int i = 0; i < EXCHANGE_AMOUNTS.length; i++) {
            if (EXCHANGE_AMOUNTS[i] == current) return EXCHANGE_AMOUNTS[(i + 1) % EXCHANGE_AMOUNTS.length];
        }
        return 64;
    }

    /** Pending exchange confirm: true = diamonds->money. count/total are
     *  re-validated (and clamped) right before the exchange is applied. */
    public static class PendingExchange {
        public final boolean diaToMoney;
        public int count;
        public double total;

        PendingExchange(boolean diaToMoney, int count, double total) {
            this.diaToMoney = diaToMoney;
            this.count = count;
            this.total = total;
        }
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
    /** viewer -> pending exchange confirm */
    private final Map<UUID, PendingExchange> pendingExchange = new HashMap<>();
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

    public void markExchange(Player p, PendingExchange pe) {
        pendingExchange.put(p.getUniqueId(), pe);
    }

    public PendingExchange takeExchange(Player p) {
        return pendingExchange.remove(p.getUniqueId());
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

    /** Drops every per-player GUI state (called on quit: prevents map leaks). */
    public void cleanup(UUID player) {
        states.remove(player);
        pendingConfirm.remove(player);
        expiredSlotMap.remove(player);
        pendingExchange.remove(player);
        pendingSearch.remove(player);
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
        String title = AhTheme.titleBrowser(st.page + 1, pages, list.size());
        if (st.mode == Mode.MY) title = AhTheme.titleMy(countMine(p), maxSlots);
        if (st.mode == Mode.SEARCH) title = AhTheme.titleSearch(st.query, st.page + 1, pages);

        Inventory inv = Bukkit.createInventory(new BrowserHolder(), 54, Msg.color(title));

        // Play opening sound
        if (cfg.sounds) {
            p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 0.5f, 1.2f);
        }

        int from = st.page * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, list.size());
        for (int i = from; i < to; i++) {
            inv.setItem(i - from, renderListing(list.get(i), p.getUniqueId()));
        }
        if (list.isEmpty()) {
            inv.setItem(22, st.mode == Mode.SEARCH && !st.query.isEmpty()
                    ? AhTheme.emptySearch(st.query) : AhTheme.emptyBrowser());
        }
        // nav row with animated gradient panes
        for (int s = 45; s < 54; s++) inv.setItem(s, animatedFiller());

        inv.setItem(45, AhTheme.navBack(st.page + 1, pages));
        boolean inMy = st.mode == Mode.MY;
        inv.setItem(46, button(Material.ENDER_CHEST,
                inMy ? "<gradient:#7DFF9A:#FFE27A>◆ Все лоты ◆</gradient>"
                     : "<gradient:#7AD9FF:#FFD76A>◆ Мои лоты ◆</gradient>",
                "<gray>Ваши: <white>" + countMine(p) + "</white><gray>/</gray><white>" + maxSlots + "</white></gray>",
                "<gray>Баланс: <gold>" + balanceOf(p) + "</gold></gray>",
                inMy ? "<gray>Клик — вернуться ко всем лотам</gray>" : "<gray>Клик — открыть</gray>"));
        inv.setItem(47, button(Material.CHEST, "<gradient:#FFA500:#FFD700>◆ Склад ◆</gradient>",
                "<gray>Забрать: <white>" + storage.vaultSize(p.getUniqueId()) + "</white></gray>",
                "<gray>Просрочка и снятые лоты</gray>", "<gray>Клик — открыть</gray>"));
        inv.setItem(48, button(Material.COMPARATOR,
                "<gradient:#7AD9FF:#FFD76A>◆ Сорт: " + sortRu(st.sort) + "◆</gradient>",
                "<gray>Дальше: <white>" + sortRu(nextSort(st.sort)) + "</white></gray>",
                "<gray>Клик — сменить</gray>"));
        inv.setItem(49, AhTheme.closeButton(balanceOf(p)));
        inv.setItem(50, button(st.category.icon,
                "<gradient:#7AD9FF:#FFD76A>◆ " + st.category.nameRu + " ◆</gradient>",
                "<gray>Клик — выбрать категорию</gray>"));
        inv.setItem(51, AhTheme.searchButton(st.query));
        inv.setItem(52, cfg.exchangeEnabled
                ? button(Material.GOLD_INGOT,
                    "<gradient:#FFD700:#FFA500>◆ Обменник ◆</gradient>",
                    "<gray>Алмазы <white>⇄</white> деньги</gray>",
                    "<gray>1 алм = <gold>" + Fmt.money(cfg.diamondPrice, cfg.symbol) + "</gold></gray>",
                    "<gray>Клик — открыть</gray>")
                : button(Material.BARRIER,
                    "<gradient:#FF6B6B:#FF8E8E>◆ Обменник ◆</gradient>",
                    "<gray>Временно выключен</gray>"));
        inv.setItem(53, AhTheme.navForward(st.page + 1, pages));
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

    private Sort nextSort(Sort s) {
        return switch (s) {
            case NEWEST -> Sort.PRICE_ASC;
            case PRICE_ASC -> Sort.PRICE_DESC;
            case PRICE_DESC -> Sort.TIME_LEFT;
            case TIME_LEFT -> Sort.NEWEST;
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
        boolean own = l.seller.equals(viewer);
        long left = l.expiresAt - System.currentTimeMillis();
        // urgency color: plenty of time / under a day / under an hour
        ItemMeta meta = base.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        // Price with gradient
        lore.add(Msg.color(AhTheme.priceLine(l.price, cfg.symbol)));
        AhTheme.Rarity rarity = AhTheme.rarity(l.price, cfg.rareAt, cfg.epicAt);
        Component badge = AhTheme.rarityLine(rarity, cfg);
        lore.add(badge);
        lore.add(Msg.color("&7Продавец: &f" + l.sellerName));
        // Time with urgency color: green -> yellow (<6h) -> red (<1h)
        String timeColor = left <= 3600_000L ? "&c" : (left <= 6 * 3600_000L ? "&e" : "&a");
        lore.add(Msg.color(timeColor + "⏳ До конца: " + Fmt.timeLeft(left)));
        if (l.enchanted) {
            lore.add(Msg.color("&d✦ Зачаровано &7(" + l.material + ")"));
        }
        if (own) {
            lore.add(Component.empty());
            lore.add(Msg.color("<gradient:#FF6B6B:#FF8E8E>◆ Ваш лот — клик = снять ◆</gradient>"));
        } else {
            lore.add(Component.empty());
            lore.add(Msg.color("<gradient:#7AD9FF:#FFD76A>◆ Клик — купить ◆</gradient>"));
        }
        // Add subtle gradient border effect via lore
        lore.add(Component.empty());
        lore.add(Msg.color(AhTheme.divider()));
        // Glow effect for own lots, ending-soon and epic items (some items reject the
        // enchant on meta level -> never let that break rendering)
        if (own || left < 3600_000L || rarity == AhTheme.Rarity.EPIC) {
            try {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } catch (IllegalArgumentException ignored) {
            }
        }
        // Add custom model data for custom textures support
        // meta.setCustomModelData(12345); // Uncomment if using resource pack
        meta.lore(lore);
        base.setItemMeta(meta);
        return base;
    }

    // ---------------- categories ----------------

    public void openCategories(Player p) {
        Inventory inv = Bukkit.createInventory(new CategoriesHolder(), 27,
                Msg.color(AhTheme.titleCategories()));
        ItemStack fill = filler();
        for (int s = 0; s < 27; s++) inv.setItem(s, fill);
        State st = stateOf(p);
        Category[] cats = Category.values();
        int slot = 9;
        for (Category c : cats) {
            boolean selected = st.category == c;
            ItemStack it = button(c.icon,
                    (selected ? "<gradient:#7DFF9A:#FFE27A>★ " + c.nameRu + "</gradient>" : "&7" + c.nameRu),
                    selected ? "&aВыбрано" : "&7Клик — выбрать");
            if (selected) {
                ItemMeta m = it.getItemMeta();
                m.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                m.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                it.setItemMeta(m);
            }
            inv.setItem(slot++, it);
        }
        inv.setItem(22, AhTheme.backButton());
        p.openInventory(inv);
    }

    // ---------------- confirm ----------------

    public void openConfirm(Player p, Listing l) {
        markConfirm(p, l.id);
        Inventory inv = Bukkit.createInventory(new ConfirmHolder(), 27, Msg.color(AhTheme.titleConfirm()));
        ItemStack fill = filler();
        for (int s = 0; s < 27; s++) inv.setItem(s, fill);
        inv.setItem(13, renderListing(l, p.getUniqueId()));
        double tax = l.price * cfg.taxPercent / 100.0;
        inv.setItem(11, button(Material.GREEN_WOOL, "&a&lКУПИТЬ за " + Fmt.money(l.price, cfg.symbol),
                "&7Товар: &f" + l.displayName,
                "&7Продавец получит &e" + Fmt.money(l.price - tax, cfg.symbol),
                "&7Ваш баланс: &e" + balanceOf(p)));
        inv.setItem(15, button(Material.RED_WOOL, "&cНазад"));
        p.openInventory(inv);
    }

    // ---------------- expired ----------------

    public void openExpired(Player p) {
        List<String> items = storage.peekBase64(p.getUniqueId());
        Inventory inv = Bukkit.createInventory(new ExpiredHolder(), 54, Msg.color(AhTheme.titleExpired(items.size())));
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
        inv.setItem(49, AhTheme.backButton());
        inv.setItem(53, button(Material.EMERALD_BLOCK, "&a&lЗабрать ВСЁ &7(&e" + items.size() + "&7)", "&7Клик — забрать всё"));
        expiredSlotMap.put(p.getUniqueId(), map);
        p.openInventory(inv);
    }

    public List<Integer> expiredMap(Player p) {
        return expiredSlotMap.getOrDefault(p.getUniqueId(), Collections.emptyList());
    }

    // ---------------- exchanger ----------------

    /** Diamonds (whole inventory) currently held. */
    public int countDiamonds(Player p) {
        int n = 0;
        try {
            for (ItemStack it : p.getInventory().getContents()) {
                if (it != null && it.getType() == Material.DIAMOND) n += it.getAmount();
            }
        } catch (Exception ignored) {}
        return n;
    }

    private double balanceOfSafe(Player p) {
        if (economy == null || !economy.ready()) return 0;
        try {
            return economy.balance(p);
        } catch (Exception e) {
            return 0;
        }
    }

    public void openExchanger(Player p) {
        if (!cfg.exchangeEnabled) {
            p.sendMessage(Msg.color(cfg.prefix + "&cОбменник выключен."));
            return;
        }
        State st = stateOf(p);
        Inventory inv = Bukkit.createInventory(new ExchangerHolder(), 27,
                Msg.color(AhTheme.titleExchanger()));
        ItemStack fill = filler();
        for (int s = 0; s < 27; s++) inv.setItem(s, fill);

        int have = countDiamonds(p);
        double price = cfg.diamondPrice;
        double bal = balanceOfSafe(p);
        int afford = price > 0 ? (int) (bal / price) : 0;

        int want = st.exchangeAmount;
        int sellN = want < 0 ? have : Math.min(want, have);
        double sellSum = sellN * price;
        int buyN = want < 0 ? afford : Math.min(want, afford);
        double buySum = buyN * price;

        inv.setItem(11, button(Material.DIAMOND, "<gradient:#7AD9FF:#FFFFFF>Алмазы → Деньги</gradient>",
                "&7У тебя алмазов: &b" + have,
                "&7Обменяешь: &b" + sellN + " &7→ &e" + Fmt.money(sellSum, cfg.symbol),
                "&71 алмаз = &e" + Fmt.money(price, cfg.symbol),
                sellN > 0 ? "&aКлик — далее" : "&cНет алмазов"));
        inv.setItem(13, button(Material.SUNFLOWER, "&eКоличество: &f" + exchangeAmountLabel(want),
                "&71 → 16 → 32 → 64 → ВСЁ", "&7Клик — сменить"));
        inv.setItem(15, button(Material.EMERALD, "<gradient:#7DFF9A:#FFE27A>Деньги → Алмазы</gradient>",
                "&7Баланс: &e" + Fmt.money(bal, cfg.symbol),
                "&7Купишь: &b" + buyN + " &7за &e" + Fmt.money(buySum, cfg.symbol),
                buyN > 0 ? "&aКлик — далее" : "&cНе хватает денег"));
        inv.setItem(22, AhTheme.backButton());
        p.openInventory(inv);
    }

    public void openExchangeConfirm(Player p, boolean diaToMoney, int n, double total) {
        markExchange(p, new PendingExchange(diaToMoney, n, total));
        Inventory inv = Bukkit.createInventory(new ExchangeConfirmHolder(), 27,
                Msg.color(AhTheme.titleExchangeConfirm()));
        ItemStack fill = filler();
        for (int s = 0; s < 27; s++) inv.setItem(s, fill);
        if (diaToMoney) {
            inv.setItem(13, button(Material.DIAMOND, "&b" + n + " алмаз.",
                    "&7Получишь: &e" + Fmt.money(total, cfg.symbol)));
        } else {
            inv.setItem(13, button(Material.EMERALD, "&a" + n + " алмаз.",
                    "&7Заплатишь: &e" + Fmt.money(total, cfg.symbol)));
        }
        inv.setItem(11, button(Material.GREEN_WOOL, "&a&lОБМЕНЯТЬ",
                "&7Назад пути не будет"));
        inv.setItem(15, button(Material.RED_WOOL, "&cОтмена"));
        p.openInventory(inv);
    }

    /** Executes a confirmed exchange. Returns message key, null = ok. */
    public String doExchange(Player p, PendingExchange pe) {
        double price = cfg.diamondPrice;
        if (price <= 0) return "&cОбменник выключен.";
        if (economy == null || !economy.ready()) return "&cНет экономики.";
        if (pe.diaToMoney) {
            int have = countDiamonds(p);
            int n = Math.min(pe.count, have);
            if (n <= 0) return "&cНет алмазов в инвентаре.";
            int rest = n;
            ItemStack[] contents = p.getInventory().getContents();
            for (int i = 0; i < contents.length && rest > 0; i++) {
                ItemStack it = contents[i];
                if (it != null && it.getType() == Material.DIAMOND) {
                    int take = Math.min(rest, it.getAmount());
                    if (take >= it.getAmount()) {
                        contents[i] = null; // never leave 0-amount stacks behind
                    } else {
                        it.setAmount(it.getAmount() - take);
                    }
                    rest -= take;
                }
            }
            p.getInventory().setContents(contents);
            // player may have dropped diamonds since the confirm screen
            pe.count = n;
            pe.total = n * price;
            if (!economy.deposit(p, pe.total)) return "&cОшибка начисления.";
            return null;
        } else {
            double cost = pe.count * price;
            if (cost <= 0) return "&cНеверная сумма.";
            if (!economy.has(p, cost)) return "&cНе хватает денег.";
            if (!economy.withdraw(p, cost)) return "&cОплата не прошла.";
            int rest = pe.count;
            while (rest > 0) {
                int stack = Math.min(rest, 64);
                Map<Integer, ItemStack> left = p.getInventory()
                        .addItem(new ItemStack(Material.DIAMOND, stack));
                for (ItemStack l : left.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), l);
                }
                rest -= stack;
            }
            return null;
        }
    }

    // ---------------- helpers ----------------

private ItemStack filler() {
        return AhTheme.filler(cfg);
    }

    /** Animated GOLD nav-row filler: nav strip reads different from content. */
    private ItemStack animatedFiller() {
        ItemStack it = new ItemStack(Material.GOLD_NUGGET);
        ItemMeta m = it.getItemMeta();
        String[] gradients = {
            "<gradient:#FFD700:#7A5C00> </gradient>",
            "<gradient:#7A5C00:#FFD700> </gradient>",
            "<gradient:#FFA500:#FFD700> </gradient>"
        };
        m.displayName(Msg.color(gradients[(int)(System.currentTimeMillis() / 2000) % 3]));
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

    public static class ExchangerHolder implements InventoryHolder {
        @Override public Inventory getInventory() { return null; }
    }

    public static class ExchangeConfirmHolder implements InventoryHolder {
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
