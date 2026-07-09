package dev.portfolio.bastion.Clans;

import dev.portfolio.bastion.Clans.ActionItem;
import dev.portfolio.bastion.Clans.ClanGlow.ClanGlowManager;
import dev.portfolio.bastion.Clans.Commands.BaseCommand;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Clans.GUIManager;
import dev.portfolio.bastion.Clans.MenuHolder;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class MenuListener
implements Listener {
    private final Bastion plugin;
    private final GUIManager gui;
    private final DataManager data;
    private final ClanGlowManager glow;
    private final BaseCommand baseCommand;

    public MenuListener(Bastion plugin, GUIManager gui, DataManager data, ClanGlowManager glow, BaseCommand baseCommand) {
        this.plugin = plugin;
        this.gui = gui;
        this.data = data;
        this.glow = glow;
        this.baseCommand = baseCommand;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String type;
        HumanEntity humanEntity = e.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        InventoryHolder inventoryHolder = e.getInventory().getHolder();
        if (!(inventoryHolder instanceof MenuHolder)) {
            return;
        }
        MenuHolder holder = (MenuHolder)inventoryHolder;
        e.setCancelled(true);
        ActionItem action = this.gui.getAction(e.getInventory(), e.getSlot());
        if (action == null) {
            return;
        }
        switch (type = action.getType()) {
            case "back": {
                if (holder.getMenuName().equalsIgnoreCase("mainClanExists") || holder.getMenuName().equalsIgnoreCase("mainClanNotExists")) {
                    for (String cmd : this.plugin.getConfig().getStringList("clanSettings.backCommand")) {
                        cmd = cmd.replace("%player%", player.getName());
                        Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)cmd);
                    }
                    break;
                }
                this.gui.openMainMenu(player);
                break;
            }
            case "membersMenu": {
                this.gui.openMenu(player, "membersMenu");
                break;
            }
            case "basesMenu": {
                this.gui.openMenu(player, "basesMenu");
                break;
            }
            case "potionsMenu": {
                this.gui.openMenu(player, "potionsMenu");
                break;
            }
            case "base": {
                this.handleBaseClick(player, action.getId());
                break;
            }
            case "potion": {
                this.handlePotionBuy(player, action.getId());
                break;
            }
            case "locator": {
                this.glow.toggleLocator(player);
                this.gui.openMenu(player, "mainClanExists");
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof MenuHolder) {
            this.gui.clearActions(e.getInventory());
        }
    }

    private void handleBaseClick(Player player, String id) {
        int current;
        String clan = this.data.getClan(player);
        if (clan == null) {
            return;
        }
        int level = Integer.parseInt(id.replace("base", ""));
        if (level <= (current = this.data.getBases(clan))) {
            Location loc = this.data.getBaseByIndex(clan, level);
            if (loc == null || loc.getWorld() == null) {
                player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.unknownBase")));
                player.closeInventory();
                return;
            }
            this.baseCommand.startTeleport(player, loc);
            player.closeInventory();
            return;
        }
        if (level != current + 1) {
            player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.baseCantBuy")));
            return;
        }
        int price = this.plugin.getGuiConfig().get().getInt("basesMenu.items." + id + ".price");
        if (!Bastion.getEcon().has((OfflinePlayer)player, (double)price)) {
            player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.noMoneyForBase")));
            return;
        }
        Bastion.getEcon().withdrawPlayer((OfflinePlayer)player, (double)price);
        this.data.setBases(clan, level);
        player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.clanSetBase")));
        this.gui.openMenu(player, "basesMenu");
    }

    private void handlePotionBuy(Player player, String id) {
        String clan = this.data.getClan(player);
        if (clan == null) {
            return;
        }
        if (this.data.getPotions(clan).contains(id)) {
            return;
        }
        int price = this.plugin.getGuiConfig().get().getInt("potionsMenu.items." + id + ".price");
        if (Bastion.getPP() == null) {
            player.sendMessage(Utils.color("&cPlayerPoints недоступен"));
            return;
        }
        if (Bastion.getPP().look(player.getUniqueId()) < price) {
            player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.noPointsForPotions")));
            return;
        }
        Bastion.getPP().take(player.getUniqueId(), price);
        this.data.addPotion(clan, id);
        player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.effectPurchase")));
    }
}
