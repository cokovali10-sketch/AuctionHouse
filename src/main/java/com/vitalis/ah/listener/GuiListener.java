package com.vitalis.ah.listener;

import com.vitalis.ah.AuctionHouse;
import com.vitalis.ah.gui.AhGui;
import com.vitalis.ah.gui.Category;
import com.vitalis.ah.model.Listing;
import com.vitalis.ah.util.Fmt;
import com.vitalis.ah.util.ItemSer;
import com.vitalis.ah.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Click handling for all AH GUIs. Everything cancelled (anti-dupe). */
public class GuiListener implements Listener {
    private final AuctionHouse plugin;

    public GuiListener(AuctionHouse plugin) {
        this.plugin = plugin;
    }

    private void clickSound(Player p) {
        if (plugin.cfg().sounds) p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1f);
    }

    private void errorSound(Player p) {
        if (plugin.cfg().sounds) p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof AhGui.BrowserHolder
                || e.getInventory().getHolder() instanceof AhGui.CategoriesHolder
                || e.getInventory().getHolder() instanceof AhGui.ConfirmHolder
                || e.getInventory().getHolder() instanceof AhGui.ExpiredHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getInventory().getHolder() instanceof AhGui.BrowserHolder) {
            e.setCancelled(true);
            handleBrowser(p, e.getRawSlot());
        } else if (e.getInventory().getHolder() instanceof AhGui.CategoriesHolder) {
            e.setCancelled(true);
            handleCategories(p, e.getRawSlot());
        } else if (e.getInventory().getHolder() instanceof AhGui.ConfirmHolder) {
            e.setCancelled(true);
            handleConfirm(p, e.getRawSlot());
        } else if (e.getInventory().getHolder() instanceof AhGui.ExpiredHolder) {
            e.setCancelled(true);
            handleExpired(p, e.getRawSlot());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof AhGui.ConfirmHolder
                && e.getPlayer() instanceof Player p) {
            plugin.gui().takeConfirm(p); // discard stale confirm
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.gui().cancelSearchInput(e.getPlayer());
    }

    /** Chat search input: player clicked the search button, next message = query. */
    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.gui().takeSearchInput(p)) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (text.isEmpty()) {
                p.sendMessage(Msg.color(plugin.cfg().prefix + "&7Поиск отменён."));
                return;
            }
            AhGui.State st = plugin.gui().stateOf(p);
            st.mode = AhGui.Mode.SEARCH;
            st.query = text;
            st.page = 0;
            plugin.gui().openBrowser(p, st);
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        int expired = plugin.storage().vaultSize(p.getUniqueId());
        if (expired > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    p.sendMessage(Msg.color(plugin.cfg().prefix
                            + "&eУ тебя &f" + expired + " &eпросроченных предметов. &7(кнопка «Склад» в /ah)")),
                    40L);
        }
    }

    // ---------------- browser ----------------

    private void handleBrowser(Player p, int slot) {
        AhGui.State st = plugin.gui().stateOf(p);
        if (slot < 45) {
            List<Listing> list = plugin.gui().view(st, p.getUniqueId());
            int idx = st.page * AhGui.PAGE_SIZE + slot;
            if (idx < 0 || idx >= list.size()) return;
            Listing l = list.get(idx);
            Listing fresh = plugin.storage().get(l.id);
            if (fresh == null) {
                p.sendMessage(Msg.color(plugin.cfg().prefix + "&cУже купили. Обновляю..."));
                errorSound(p);
                plugin.gui().openBrowser(p, st);
                return;
            }
            if (l.seller.equals(p.getUniqueId())) {
                plugin.storage().remove(l.id);
                plugin.storage().stashBase64(p.getUniqueId(), l.itemBase64);
                plugin.storage().saveAsync();
                p.sendMessage(Msg.color(plugin.cfg().prefix + "&eЛот снят. Предмет на складе (кнопка «Склад»)."));
                clickSound(p);
                plugin.gui().openBrowser(p, st);
                return;
            }
            clickSound(p);
            plugin.gui().openConfirm(p, fresh);
            return;
        }
        switch (slot) {
            case 45 -> {
                if (st.page > 0) {
                    st.page--;
                    if (plugin.cfg().sounds) p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1f);
                }
                plugin.gui().openBrowser(p, st);
            }
            case 53 -> {
                st.page++;
                if (plugin.cfg().sounds) p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1f);
                plugin.gui().openBrowser(p, st);
            }
            case 46 -> {
                st.mode = AhGui.Mode.MY;
                st.page = 0;
                clickSound(p);
                plugin.gui().openBrowser(p, st);
            }
            case 47 -> {
                clickSound(p);
                plugin.gui().openExpired(p);
            }
            case 48 -> {
                st.sort = switch (st.sort) {
                    case NEWEST -> AhGui.Sort.PRICE_ASC;
                    case PRICE_ASC -> AhGui.Sort.PRICE_DESC;
                    case PRICE_DESC -> AhGui.Sort.TIME_LEFT;
                    case TIME_LEFT -> AhGui.Sort.NEWEST;
                };
                clickSound(p);
                plugin.gui().openBrowser(p, st);
            }
            case 49 -> p.closeInventory();
            case 50 -> {
                clickSound(p);
                plugin.gui().openCategories(p);
            }
            case 51 -> {
                // chat search
                p.closeInventory();
                plugin.gui().markSearchInput(p);
                p.sendMessage(Msg.color(plugin.cfg().prefix + "&eНапиши запрос в чат &7(название, материал, продавец)."));
                p.sendMessage(Msg.color(plugin.cfg().prefix + "&7Пустое сообщение = отмена. Есть 30 сек."));
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.gui().takeSearchInput(p)) {
                        p.sendMessage(Msg.color(plugin.cfg().prefix + "&7Поиск отменён (таймаут)."));
                    }
                }, 30L * 20L);
            }
            case 52 -> {
                clickSound(p);
                plugin.gui().openBrowser(p, st); // refresh (or admin indicator)
            }
            default -> {}
        }
    }

    // ---------------- categories ----------------

    private void handleCategories(Player p, int slot) {
        if (slot == 22) {
            AhGui.State st = plugin.gui().stateOf(p);
            clickSound(p);
            plugin.gui().openBrowser(p, st);
            return;
        }
        int idx = slot - 9;
        Category[] cats = Category.values();
        if (idx < 0 || idx >= cats.length) return;
        AhGui.State st = plugin.gui().stateOf(p);
        st.category = cats[idx];
        st.page = 0;
        clickSound(p);
        plugin.gui().openBrowser(p, st);
    }

    // ---------------- confirm ----------------

    private void handleConfirm(Player p, int slot) {
        if (slot == 15) {
            AhGui.State st = plugin.gui().stateOf(p);
            plugin.gui().takeConfirm(p);
            clickSound(p);
            plugin.gui().openBrowser(p, st);
            return;
        }
        if (slot != 11) return;
        UUID id = plugin.gui().takeConfirm(p);
        if (id == null) {
            p.closeInventory();
            return;
        }
        Listing l = plugin.storage().remove(id); // atomic
        if (l == null) {
            p.sendMessage(Msg.color(plugin.cfg().prefix + "&cЭтот лот только что купили."));
            errorSound(p);
            p.closeInventory();
            return;
        }
        if (l.seller.equals(p.getUniqueId())) {
            plugin.storage().put(l); // put back
            p.sendMessage(Msg.color(plugin.cfg().prefix + "&cСвой лот купить нельзя."));
            errorSound(p);
            p.closeInventory();
            return;
        }
        if (!plugin.economy().has(p, l.price)) {
            plugin.storage().put(l); // put back
            p.sendMessage(Msg.color(plugin.cfg().prefix + "&cНе хватает денег. Нужно &e"
                    + Fmt.money(l.price, plugin.cfg().symbol)));
            errorSound(p);
            p.closeInventory();
            return;
        }
        ItemStack item;
        try {
            item = ItemSer.fromBase64(l.itemBase64);
        } catch (Exception ex) {
            plugin.storage().put(l);
            p.sendMessage(Msg.color(plugin.cfg().prefix + "&cБитые данные предмета, сообщи админу."));
            errorSound(p);
            p.closeInventory();
            return;
        }
        if (!plugin.economy().withdraw(p, l.price)) {
            plugin.storage().put(l);
            p.sendMessage(Msg.color(plugin.cfg().prefix + "&cОплата не прошла."));
            errorSound(p);
            p.closeInventory();
            return;
        }
        double tax = l.price * plugin.cfg().taxPercent / 100.0;
        double payout = l.price - tax;
        org.bukkit.OfflinePlayer seller = Bukkit.getOfflinePlayer(l.seller);
        plugin.economy().deposit(seller, payout);

        Map<Integer, ItemStack> left = p.getInventory().addItem(item);
        for (ItemStack rest : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), rest);
        }
        plugin.storage().saveAsync();
        p.sendMessage(Msg.color(plugin.cfg().prefix + "&aКуплено &f" + AhGui.prettyName(item)
                + " &aза &e" + Fmt.money(l.price, plugin.cfg().symbol)));
        if (plugin.cfg().sounds) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1f);
        p.closeInventory();

        if (plugin.cfg().soldMessage && seller.isOnline() && seller.getPlayer() != null) {
            seller.getPlayer().sendMessage(Msg.color(plugin.cfg().prefix
                    + "&aТвой &f" + AhGui.prettyName(item) + " &aкуплен за &e"
                    + Fmt.money(payout, plugin.cfg().symbol) + " &7(комиссия " + plugin.cfg().taxPercent + "%)"));
        }
    }

    // ---------------- expired ----------------

    private void handleExpired(Player p, int slot) {
        if (slot == 49) {
            AhGui.State st = plugin.gui().stateOf(p);
            clickSound(p);
            plugin.gui().openBrowser(p, st);
            return;
        }
        if (slot == 53) {
            collectAll(p);
            return;
        }
        if (slot >= 45) return;
        List<Integer> map = plugin.gui().expiredMap(p);
        int pos = map.indexOf(slot);
        if (pos < 0) return;
        List<ItemStack> all = plugin.storage().takeAll(p.getUniqueId());
        if (pos >= all.size()) {
            plugin.gui().openExpired(p);
            return;
        }
        ItemStack chosen = all.remove(pos);
        for (ItemStack rest : all) {
            plugin.storage().stash(p.getUniqueId(), rest);
        }
        Map<Integer, ItemStack> left = p.getInventory().addItem(chosen);
        for (ItemStack l : left.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), l);
        }
        plugin.storage().saveAsync();
        p.sendMessage(Msg.color(plugin.cfg().prefix + "&aЗабрано: &f" + AhGui.prettyName(chosen)));
        clickSound(p);
        plugin.gui().openExpired(p);
    }

    private void collectAll(Player p) {
        List<ItemStack> all = plugin.storage().takeAll(p.getUniqueId());
        if (all.isEmpty()) {
            p.sendMessage(Msg.color(plugin.cfg().prefix + "&7Склад пуст."));
            errorSound(p);
            return;
        }
        for (ItemStack it : all) {
            Map<Integer, ItemStack> left = p.getInventory().addItem(it);
            for (ItemStack l : left.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), l);
            }
        }
        plugin.storage().saveAsync();
        p.sendMessage(Msg.color(plugin.cfg().prefix + "&aЗабрано предметов: &e" + all.size()));
        if (plugin.cfg().sounds) p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1f);
        p.closeInventory();
    }
}
