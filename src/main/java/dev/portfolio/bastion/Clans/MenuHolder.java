package dev.portfolio.bastion.Clans;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MenuHolder
implements InventoryHolder {
    private final String menuName;
    private Inventory inventory;

    public MenuHolder(String menuName, int size, String title) {
        this.menuName = menuName;
        this.inventory = Bukkit.createInventory((InventoryHolder)this, (int)size, (String)title);
    }

    public Inventory getInventory() {
        return this.inventory;
    }

    public String getMenuName() {
        return this.menuName;
    }
}
