package dev.portfolio.bastion.Clans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.portfolio.bastion.Clans.ActionItem;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Clans.MenuHolder;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GUIManager {
    private final Bastion plugin;
    private final DataManager data;
    private final Map<Inventory, Map<Integer, ActionItem>> actions = new HashMap<Inventory, Map<Integer, ActionItem>>();

    public GUIManager(Bastion plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void openMainMenu(Player player) {
        if (this.data.isInClan(player)) {
            this.openMenu(player, "mainClanExists");
        } else {
            this.openMenu(player, "mainClanNotExists");
        }
    }

    public void openMenu(Player player, String menuName) {
        ConfigurationSection section = this.plugin.getGuiConfig().get().getConfigurationSection(menuName);
        if (section == null) {
            return;
        }
        MenuHolder holder = new MenuHolder(menuName, section.getInt("size"), Utils.color(section.getString("title")));
        Inventory inv = holder.getInventory();
        if (menuName.equals("membersMenu")) {
            this.fillMembers(player, inv, section);
        }
        HashMap<Integer, ActionItem> map = new HashMap<Integer, ActionItem>();
        ConfigurationSection items = section.getConfigurationSection("items");
        for (String key : items.getKeys(false)) {
            ConfigurationSection itemSec = items.getConfigurationSection(key);
            ItemStack item = key.matches("base\\d+") || key.matches("potions\\d+") ? this.buildSpecialItem(player, key, itemSec) : this.buildItem(itemSec, player, null);
            String type = itemSec.getString("type");
            for (int slot : this.parseSlots(itemSec.get("slots"))) {
                inv.setItem(slot, item);
                if (type != null) {
                    map.put(slot, new ActionItem(type, key));
                }
                if (key.matches("base\\d+")) {
                    map.put(slot, new ActionItem("base", key));
                }
                if (!key.matches("potions\\d+")) continue;
                map.put(slot, new ActionItem("potion", key));
            }
        }
        this.actions.put(inv, map);
        player.openInventory(inv);
    }

    private ItemStack buildSpecialItem(Player player, String key, ConfigurationSection sec) {
        String clan = this.data.getClan(player);
        if (clan == null) {
            return new ItemStack(Material.BARRIER);
        }
        if (key.matches("base\\d+")) {
            ConfigurationSection active;
            int level = Integer.parseInt(key.replace("base", ""));
            int current = this.data.getBases(clan);
            String baseName = this.data.getBaseNameByIndex(clan, level);
            if (level == 1 && (active = sec.getConfigurationSection("active")) != null) {
                return this.buildItem(active, player, baseName);
            }
            if (level <= current) {
                active = sec.getConfigurationSection("active");
                if (active != null) {
                    return this.buildItem(active, player, baseName);
                }
            } else {
                ConfigurationSection inactive = sec.getConfigurationSection("inactive");
                if (inactive != null) {
                    return this.buildItem(inactive, player, null);
                }
            }
        }
        if (key.matches("potions\\d+")) {
            List<String> potions = this.data.getPotions(clan);
            if (potions.contains(key)) {
                ConfigurationSection active = sec.getConfigurationSection("active");
                if (active != null) {
                    return this.buildItem(active, player, null);
                }
            } else {
                ConfigurationSection inactive = sec.getConfigurationSection("inactive");
                if (inactive != null) {
                    return this.buildItem(inactive, player, null);
                }
            }
        }
        return new ItemStack(Material.BARRIER);
    }

    private ItemStack buildItem(ConfigurationSection sec, Player player, String baseName) {
        String material;
        String clan;
        if (sec.contains("active") && sec.contains("inactive") && (clan = this.data.getClan(player)) != null) {
            ConfigurationSection chosen;
            boolean active = this.plugin.getGlowManager().isActive(clan);
            ConfigurationSection configurationSection = chosen = active ? sec.getConfigurationSection("active") : sec.getConfigurationSection("inactive");
            if (chosen != null) {
                sec = chosen;
            }
        }
        if ((material = sec.getString("material")) == null) {
            material = "STONE";
        }
        ItemStack item = material.equalsIgnoreCase("PLAYER_HEAD") && sec.contains("value") ? Utils.createHead(sec.getString("value")) : new ItemStack(Material.valueOf((String)material.toUpperCase()));
        ItemMeta meta = item.getItemMeta();
        String name = sec.getString("name");
        if (name != null) {
            name = this.replaceStats(player, name);
            if (baseName != null) {
                name = name.replace("{baseName}", baseName);
            }
            meta.setDisplayName(Utils.color(name));
        }
        if (sec.contains("lore")) {
            ArrayList lore = new ArrayList();
            for (String line : sec.getStringList("lore")) {
                line = this.replaceStats(player, line);
                if (baseName != null) {
                    line = line.replace("{baseName}", baseName);
                }
                lore.add(Utils.color(line));
            }
            meta.setLore((List)lore);
        }
        if (sec.contains("item_flags")) {
            for (String flag : sec.getStringList("item_flags")) {
                try {
                    meta.addItemFlags(new ItemFlag[]{ItemFlag.valueOf((String)flag.toUpperCase())});
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Неизвестный флаг: " + flag);
                }
            }
        }
        if (sec.contains("enchants")) {
            ConfigurationSection enchSec = sec.getConfigurationSection("enchants");
            for (String enchName : enchSec.getKeys(false)) {
                try {
                    Enchantment ench = Enchantment.getByName((String)enchName.toUpperCase());
                    int level = enchSec.getInt(enchName);
                    if (ench == null) continue;
                    meta.addEnchant(ench, level, true);
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Неизвестное зачарование: " + enchName);
                }
            }
        }
        if (sec.contains("potion-effects") && item.getType() == Material.POTION) {
            PotionMeta potionMeta = (PotionMeta)meta;
            for (String line : sec.getStringList("potion-effects")) {
                try {
                    String[] split = line.split(";");
                    PotionEffectType type = PotionEffectType.getByName((String)split[0].toUpperCase());
                    int duration = Integer.parseInt(split[1]) * 20;
                    int amplifier = Integer.parseInt(split[2]) - 1;
                    if (type == null) continue;
                    potionMeta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("Неизвестный эффект: " + line);
                }
            }
            item.setItemMeta((ItemMeta)potionMeta);
            return item;
        }
        item.setItemMeta(meta);
        return item;
    }

    public void clearActions(Inventory inv) {
        this.actions.remove(inv);
    }

    public ActionItem getAction(Inventory inv, int slot) {
        if (!this.actions.containsKey(inv)) {
            return null;
        }
        return this.actions.get(inv).get(slot);
    }

    public List<Integer> parseSlots(Object obj) {
        ArrayList<Integer> slots = new ArrayList<Integer>();
        if (obj instanceof Integer) {
            slots.add((Integer)obj);
        }
        if (obj instanceof List) {
            for (Object o : (List)obj) {
                if (o.toString().contains("-")) {
                    String[] split = o.toString().split("-");
                    int start = Integer.parseInt(split[0]);
                    int end = Integer.parseInt(split[1]);
                    for (int i = start; i <= end; ++i) {
                        slots.add(i);
                    }
                    continue;
                }
                slots.add(Integer.parseInt(o.toString()));
            }
        }
        return slots;
    }

    public void fillMembers(Player player, Inventory inv, ConfigurationSection section) {
        String clan = this.data.getClan(player);
        if (clan == null) {
            return;
        }
        List<Integer> slots = this.parseSlots(section.get("memberSlots"));
        ArrayList<String> members = new ArrayList<String>(this.data.getMembers(clan));
        members.add(this.data.getOwner(clan));
        int i = 0;
        for (String uuidStr : members) {
            if (i >= slots.size()) break;
            UUID uuid = UUID.fromString(uuidStr);
            OfflinePlayer target = Bukkit.getOfflinePlayer((UUID)uuid);
            String playerName = target.getName();
            if (playerName == null) {
                playerName = "Unknown";
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta)head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.setDisplayName("§a" + playerName);
            if (uuidStr.equals(this.data.getOwner(clan))) {
                meta.setLore(List.of("§eВладелец"));
            } else {
                meta.setLore(List.of("§7Участник"));
            }
            head.setItemMeta((ItemMeta)meta);
            inv.setItem(slots.get(i).intValue(), head);
            ++i;
        }
    }

    private String replaceStats(Player player, String text) {
        String clan = this.data.getClan(player);
        if (clan == null) {
            return text;
        }
        ArrayList<String> members = new ArrayList<String>(this.data.getMembers(clan));
        members.add(this.data.getOwner(clan));
        int totalKills = 0;
        int totalDeaths = 0;
        long totalTicks = 0L;
        double totalBalance = 0.0;
        for (String uuidStr : members) {
            UUID uuid = UUID.fromString(uuidStr);
            OfflinePlayer p = Bukkit.getOfflinePlayer((UUID)uuid);
            totalKills += p.getStatistic(Statistic.PLAYER_KILLS);
            totalDeaths += p.getStatistic(Statistic.DEATHS);
            totalTicks += (long)p.getStatistic(Statistic.PLAY_ONE_MINUTE);
            if (p.isOnline()) {
                totalBalance += Bastion.getEcon().getBalance(p);
                continue;
            }
            totalBalance += Bastion.getEcon().getBalance(p);
        }
        long totalHours = totalTicks / 20L / 60L / 60L;
        return text.replace("{members}", String.valueOf(members.size())).replace("{kills}", String.valueOf(totalKills)).replace("{deaths}", String.valueOf(totalDeaths)).replace("{hours}", String.valueOf(totalHours)).replace("{balance}", String.format("%.0f", totalBalance));
    }
}
