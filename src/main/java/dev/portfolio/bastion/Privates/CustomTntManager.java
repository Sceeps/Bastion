package dev.portfolio.bastion.Privates;

import java.util.List;
import dev.portfolio.bastion.Privates.ConfigManager;
import dev.portfolio.bastion.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomTntManager {
    private final ConfigManager config;
    private final NamespacedKey key;

    public CustomTntManager(JavaPlugin plugin, ConfigManager config1) {
        this.key = new NamespacedKey((Plugin)plugin, "custom_tnt");
        this.config = config1;
        this.registerRecipe();
    }

    public ItemStack createTnt() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.color(this.config.getString("settings.custom_tnt.name")));
        List<String> lore = this.config.getStringList("settings.custom_tnt.lore");
        meta.setLore(lore.stream().map(Utils::color).toList());
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
        meta.getPersistentDataContainer().set(this.key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private void registerRecipe() {
        Bukkit.removeRecipe((NamespacedKey)this.key);
        ShapedRecipe recipe = new ShapedRecipe(this.key, this.createTnt());
        recipe.shape(new String[]{"GSG", "SOS", "GSG"});
        recipe.setIngredient('S', Material.SAND);
        recipe.setIngredient('G', Material.GUNPOWDER);
        recipe.setIngredient('O', Material.OBSIDIAN);
        Bukkit.addRecipe((Recipe)recipe);
    }

    public void unregisterRecipe() {
        Bukkit.removeRecipe((NamespacedKey)this.key);
    }

    public boolean isCustom(ItemStack item) {
        if (item == null || item.getType() != Material.TNT) {
            return false;
        }
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(this.key, PersistentDataType.BYTE);
    }
}
