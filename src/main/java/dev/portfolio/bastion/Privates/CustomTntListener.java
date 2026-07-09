package dev.portfolio.bastion.Privates;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.HashSet;
import java.util.Set;
import dev.portfolio.bastion.Privates.ConfigManager;
import dev.portfolio.bastion.Privates.CustomTntManager;
import dev.portfolio.bastion.Privates.HologramService;
import dev.portfolio.bastion.Privates.RegionService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CustomTntListener
implements Listener {
    final JavaPlugin plugin;
    private final ConfigManager config;
    private final CustomTntManager manager;
    private final RegionService regionService;
    private final HologramService hologramService;
    private final NamespacedKey key;
    private final Set<String> destroyedRegions = new HashSet<String>();
    private final Set<Material> hardBlocks = Set.of(Material.OBSIDIAN, Material.CRYING_OBSIDIAN, Material.ENDER_CHEST, Material.ANCIENT_DEBRIS);

    public CustomTntListener(JavaPlugin plugin, ConfigManager config, CustomTntManager manager, RegionService regionService, HologramService hologramService) {
        this.plugin = plugin;
        this.config = config;
        this.manager = manager;
        this.key = new NamespacedKey((Plugin)plugin, "custom_tnt");
        this.regionService = regionService;
        this.hologramService = hologramService;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (!this.manager.isCustom(item)) {
            return;
        }
        Block block = e.getBlockPlaced();
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        container.set(this.getBlockKey(block.getLocation()), PersistentDataType.STRING, "custom");
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent e) {
        NamespacedKey blockKey;
        Block block = e.getBlock();
        if (block.getType() != Material.TNT) {
            return;
        }
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        if (!container.has(blockKey = this.getBlockKey(block.getLocation()), PersistentDataType.STRING)) {
            return;
        }
        e.setDropItems(false);
        container.remove(blockKey);
        block.getWorld().dropItemNaturally(block.getLocation(), this.manager.createTnt());
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onSpawn(EntitySpawnEvent e) {
        NamespacedKey blockKey;
        if (e.getEntityType() != EntityType.TNT) {
            return;
        }
        TNTPrimed tnt = (TNTPrimed)e.getEntity();
        Location loc = tnt.getLocation();
        Block block = loc.getBlock();
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        if (!container.has(blockKey = this.getBlockKey(block.getLocation()), PersistentDataType.STRING)) {
            return;
        }
        tnt.getPersistentDataContainer().set(this.key, PersistentDataType.BYTE, (byte) 1);
        container.remove(blockKey);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onExplode(EntityExplodeEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof TNTPrimed)) {
            return;
        }
        TNTPrimed tnt = (TNTPrimed)entity;
        PersistentDataContainer data = tnt.getPersistentDataContainer();
        if (!data.has(this.key, PersistentDataType.BYTE)) {
            return;
        }
        this.destroyedRegions.clear();
        double chance = this.config.getDouble("settings.custom_tnt.chance");
        Location center = e.getLocation();
        int radius = 3;
        e.blockList().clear();
        tnt.setYield(0.0f);
        for (int x = -radius; x <= radius; ++x) {
            for (int y = -radius; y <= radius; ++y) {
                for (int z = -radius; z <= radius; ++z) {
                    Block block;
                    Material type;
                    Location loc = center.clone().add((double)x, (double)y, (double)z);
                    if (loc.distance(center) > (double)radius || (type = (block = loc.getBlock()).getType()) == Material.AIR) continue;
                    this.handlePrivateDestroy(block);
                    if (type == Material.WATER) {
                        block.setType(Material.AIR);
                        continue;
                    }
                    if (type == Material.BEDROCK) continue;
                    if (this.hardBlocks.contains(type)) {
                        if (!(Math.random() * 100.0 <= chance)) continue;
                        e.blockList().add(block);
                        continue;
                    }
                    e.blockList().add(block);
                }
            }
        }
    }

    private NamespacedKey getBlockKey(Location loc) {
        return new NamespacedKey((Plugin)this.plugin, "tnt_" + loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ());
    }

    private void handlePrivateDestroy(Block block) {
        ProtectedRegion region = this.regionService.getRegionByBlock(block.getLocation());
        if (region == null) {
            return;
        }
        String regionId = region.getId();
        if (this.destroyedRegions.contains(regionId)) {
            return;
        }
        this.destroyedRegions.add(regionId);
        this.hologramService.remove(regionId);
        try {
            this.regionService.getManager(block.getWorld()).removeRegion(regionId);
            this.regionService.getManager(block.getWorld()).save();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
