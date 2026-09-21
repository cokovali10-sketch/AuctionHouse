package com.vitalis.ah.command;

import com.vitalis.ah.AuctionHouse;
import com.vitalis.ah.gui.AhGui;
import com.vitalis.ah.model.Listing;
import com.vitalis.ah.util.Fmt;
import com.vitalis.ah.util.ItemSer;
import com.vitalis.ah.util.Msg;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.*;

/**
 * Only 3 commands: /ah, /ah sell, /ah search.
 * Everything else (my lots, storage, sort, categories) lives in GUI buttons.
 */
public class AhCommand implements CommandExecutor, TabCompleter {
    private final AuctionHouse plugin;

    public AhCommand(AuctionHouse plugin) {
        this.plugin = plugin;
    }

    private void send(Player p, String msg) {
        p.sendMessage(Msg.color(plugin.cfg().prefix + msg));
    }

    private void usage(Player p) {
        send(p, "&e/ah &7— открыть аукцион");
        send(p, "&e/ah sell <цена> &7— продать предмет в руке");
        send(p, "&e/ah search <текст> &7— найти лоты");
        send(p, "&7Остальное — кнопками в окне.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players.");
            return true;
        }
        if (!p.hasPermission("auctionhouse.use")) {
            send(p, "&cНет доступа.");
            return true;
        }
        if (args.length == 0) {
            AhGui.State st = plugin.gui().stateOf(p);
            st.mode = AhGui.Mode.ALL;
            st.page = 0;
            plugin.gui().openBrowser(p, st);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("sell")) {
            handleSell(p, args);
            return true;
        }
        if (sub.equals("search")) {
            if (!p.hasPermission("auctionhouse.search")) {
                send(p, "&cНет доступа.");
                return true;
            }
            if (args.length < 2) {
                send(p, "&eИспользование: &f/ah search <текст>");
                return true;
            }
            String q = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            AhGui.State st = plugin.gui().stateOf(p);
            st.mode = AhGui.Mode.SEARCH;
            st.query = q;
            st.page = 0;
            plugin.gui().openBrowser(p, st);
            return true;
        }
        usage(p);
        return true;
    }

    private void handleSell(Player p, String[] args) {
        if (!p.hasPermission("auctionhouse.sell")) {
            send(p, "&cНет доступа.");
            return;
        }
        if (args.length < 2) {
            send(p, "&eИспользование: &f/ah sell <цена> &7— держи предмет в руке");
            return;
        }
        double price;
        try {
            price = Double.parseDouble(args[1].replace(",", "."));
        } catch (NumberFormatException e) {
            send(p, "&cПлохая цена. Пример: /ah sell 1500");
            return;
        }
        if (price < plugin.cfg().minPrice || price > plugin.cfg().maxPrice) {
            send(p, "&cЦена должна быть от &e" + Fmt.money(plugin.cfg().minPrice, plugin.cfg().symbol)
                    + " &cдо &e" + Fmt.money(plugin.cfg().maxPrice, plugin.cfg().symbol));
            return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR) {
            send(p, "&cВозьми предмет в основную руку.");
            return;
        }
        if (plugin.cfg().blocked.contains(hand.getType())) {
            send(p, "&cЭтот предмет нельзя продавать.");
            return;
        }
        if (!plugin.cfg().allowDamaged && hand.getItemMeta() instanceof Damageable d && d.hasDamage() && d.getDamage() > 0) {
            send(p, "&cПовреждённые предметы запрещены.");
            return;
        }
        int maxSlots = com.vitalis.ah.util.Slots.maxFor(p, plugin.cfg().maxPerPlayer);
        int mine = plugin.storage().countBy(p.getUniqueId());
        if (mine >= maxSlots) {
            send(p, "&cСлоты заняты: &e" + mine + "/" + maxSlots + "&c. Сними что-то через кнопку «Мои лоты».");
            return;
        }
        if (plugin.cfg().blockedWorlds.contains(p.getWorld().getName().toLowerCase(java.util.Locale.ROOT))) {
            send(p, "&cВ этом мире продавать нельзя.");
            return;
        }
        ItemStack copy = hand.clone();
        String pretty = AhGui.prettyName(copy);
        boolean enchanted = !copy.getEnchantments().isEmpty();
        long now = System.currentTimeMillis();
        Listing l = new Listing(UUID.randomUUID(), p.getUniqueId(), p.getName(), price, now,
                now + plugin.cfg().durationSeconds * 1000L,
                ItemSer.toBase64(copy), copy.getType().name(), pretty, enchanted);
        plugin.storage().put(l);
        p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        plugin.storage().saveAsync();
        if (plugin.cfg().sounds) {
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        }

        double taxPreview = price * plugin.cfg().taxPercent / 100.0;
        send(p, "&aВыставлено &f" + pretty + " &aза &e" + Fmt.money(price, plugin.cfg().symbol)
                + " &7(комиссия при продаже " + plugin.cfg().taxPercent + "% ≈ " + Fmt.money(taxPreview, plugin.cfg().symbol) + ")");
        send(p, "&7Срок — 7 дней. Управление — кнопка «Мои лоты» в окне /ah");
        if (plugin.cfg().announceMinPrice > 0 && price >= plugin.cfg().announceMinPrice) {
            plugin.getServer().broadcast(Msg.color(plugin.cfg().prefix
                    + "&e" + p.getName() + " &7выставил &f" + pretty + " &7за &e" + Fmt.money(price, plugin.cfg().symbol)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return filter(args[0], List.of("sell", "search"));
        }
        return Collections.emptyList();
    }

    private List<String> filter(String prefix, List<String> opts) {
        String q = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : opts) {
            if (o.toLowerCase(Locale.ROOT).startsWith(q)) out.add(o);
        }
        return out;
    }
}
