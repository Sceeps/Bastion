package dev.portfolio.bastion.Privates;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Privates.ConfigManager;
import dev.portfolio.bastion.Privates.HologramService;
import dev.portfolio.bastion.Privates.RegionService;
import dev.portfolio.bastion.Privates.ToggleManager;
import dev.portfolio.bastion.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PrivateListener
implements Listener {
    private final ToggleManager toggleManager;
    private final ConfigManager config;
    private final RegionService regionService;
    private final HologramService hologramService;
    private final DataManager dataManager;
    private final Map<String, Long> notifyCooldown = new HashMap<String, Long>();
    private final Map<UUID, String> playerRegions = new HashMap<UUID, String>();
    private final Map<UUID, BossBar> bossBars = new HashMap<UUID, BossBar>();
    private final NamespacedKey key = new NamespacedKey((Plugin)Bastion.getInstance(), "custom_tnt");

    public PrivateListener(ToggleManager toggleManager, ConfigManager config, RegionService regionService, HologramService hologramService, DataManager dataManager) {
        this.toggleManager = toggleManager;
        this.config = config;
        this.regionService = regionService;
        this.hologramService = hologramService;
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        this.removeBossBar(e.getPlayer());
        this.playerRegions.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        boolean isMember;
        String previousId;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockY() == e.getTo().getBlockY() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()) {
            return;
        }
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();
        ProtectedRegion region = this.regionService.getRegionAt(e.getTo());
        String currentId = region != null ? region.getId() : null;
        if (Objects.equals(currentId, previousId = this.playerRegions.get(uuid))) {
            return;
        }
        if (previousId != null) {
            this.removeBossBar(player);
        }
        if (currentId == null) {
            this.playerRegions.remove(uuid);
            return;
        }
        this.playerRegions.put(uuid, currentId);
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        boolean bl = isMember = region.isOwner(localPlayer) || region.isMember(localPlayer);
        if (isMember) {
            return;
        }
        this.showBossBar(player);
        long now = System.currentTimeMillis();
        for (UUID ownerId : region.getOwners().getUniqueIds()) {
            String key;
            Player owner = Bukkit.getPlayer((UUID)ownerId);
            if (owner == null || !owner.isOnline() || this.notifyCooldown.containsKey(key = String.valueOf(ownerId) + ":" + region.getId()) && now - this.notifyCooldown.get(key) < 5000L) continue;
            this.notifyCooldown.put(key, now);
            BlockVector3 center = this.regionService.getCenter(region);
            owner.sendMessage(Utils.color(this.config.getString("messages.privateWarning").replace("{x}", String.valueOf(center.x())).replace("{y}", String.valueOf(center.y())).replace("{z}", String.valueOf(center.z()))));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.toggleManager.isEnabled(player)) {
            return;
        }
        Material mat = event.getBlock().getType();
        if (!this.config.isPrivateBlock(mat)) {
            return;
        }
        int radius = this.config.getRadius(mat);
        Location loc = event.getBlock().getLocation();
        if (!this.regionService.hasLimit(player)) {
            player.sendMessage(Utils.color(this.config.getString("messages.privatesLimit").replace("{limit}", String.valueOf(this.config.getLimit(player))), (OfflinePlayer)player));
            event.setCancelled(true);
            return;
        }
        if (!this.regionService.canCreate(player, loc, radius)) {
            player.sendMessage(Utils.color(this.config.getString("messages.privatesConflict"), (OfflinePlayer)player));
            event.setCancelled(true);
            return;
        }
        RegionManager manager = this.regionService.getManager(player.getWorld());
        if (manager == null) {
            player.sendMessage(Utils.color(this.config.getString("messages.privatesConflict"), (OfflinePlayer)player));
            event.setCancelled(true);
            return;
        }
        String id = this.regionService.generateId(loc);
        this.regionService.createRegion(player, loc, radius, id);
        try {
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(this.config.getSound()));
            if (sound != null && loc.getWorld() != null) {
                loc.getWorld().playSound(loc, sound, 1.0f, 1.0f);
            }
        } catch (Exception ignored) {
        }
        ProtectedRegion region = manager.getRegion(id);

        if (region != null && this.dataManager.isInClan(player)) {
            String clan = this.dataManager.getClan(player);
            String ownerUUID = player.getUniqueId().toString();
            String clanOwner = this.dataManager.getOwner(clan);
            if (clanOwner != null && !clanOwner.equals(ownerUUID)) {
                try {
                    region.getMembers().addPlayer(UUID.fromString(clanOwner));
                } catch (IllegalArgumentException ignored) {
                }
            }
            for (String member : this.dataManager.getMembers(clan)) {
                if (member.equals(ownerUUID)) continue;
                try {
                    region.getMembers().addPlayer(UUID.fromString(member));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        try {
            manager.save();
        } catch (StorageException ex) {
            ex.printStackTrace();
        }
        this.hologramService.create(id, loc, player, mat);
        this.hologramService.update(id, region, player);
        player.sendMessage(Utils.color(this.config.getString("messages.privateCreate"), (OfflinePlayer)player));
        this.spawnPrivateParticles(loc, mat);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        ProtectedRegion region = this.regionService.getRegionByBlock(loc);
        if (region == null) {
            return;
        }
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
        if (!region.isOwner(localPlayer)) {
            event.setCancelled(true);
            return;
        }
        RegionManager manager = this.regionService.getManager(loc.getWorld());
        if (manager == null) {
            return;
        }
        manager.removeRegion(region.getId());
        if (DHAPI.getHologram(region.getId()) != null) {
            DHAPI.removeHologram(region.getId());
        }
        try {
            manager.save();
        } catch (StorageException ex) {
            ex.printStackTrace();
        }
        player.sendMessage(Utils.color(this.config.getString("messages.privateRemove"), (OfflinePlayer)player));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onExplosion(EntityExplodeEvent e) {
        Iterator iterator = e.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = (Block)iterator.next();
            ProtectedRegion region = this.regionService.getRegionByBlock(block.getLocation());
            if (region == null || this.isAllowedExplosion(e.getEntity())) continue;
            iterator.remove();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block block : e.getBlocks()) {
            ProtectedRegion region = this.regionService.getRegionByBlock(block.getLocation());
            if (region == null) continue;
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block block : e.getBlocks()) {
            ProtectedRegion region = this.regionService.getRegionByBlock(block.getLocation());
            if (region == null) continue;
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onFireSpread(BlockFromToEvent e) {
        ProtectedRegion region = this.regionService.getRegionByBlock(e.getToBlock().getLocation());
        if (region != null) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityChange(EntityChangeBlockEvent e) {
        ProtectedRegion region = this.regionService.getRegionByBlock(e.getBlock().getLocation());
        if (region == null) {
            return;
        }
        if (e.getEntityType().name().contains("WITHER") || e.getEntityType().name().contains("ENDER_DRAGON")) {
            e.setCancelled(true);
        }
    }

    private boolean isAllowedExplosion(Entity entity) {
        if (entity instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed)entity;
            return tnt.getPersistentDataContainer().has(this.key, PersistentDataType.BYTE);
        }
        if (entity instanceof Creeper) {
            return false;
        }
        if (entity.getType().name().contains("WITHER")) {
            return false;
        }
        if (entity instanceof Fireball) {
            return false;
        }
        if (entity instanceof EnderCrystal) {
            return false;
        }
        return false;
    }

    private void spawnPrivateParticles(final Location center, final Material mat) {
        final World world = center.getWorld();
        if (world == null) {
            return;
        }
        final Location base = center.clone().add(0.5, 0.2, 0.5);
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (this.ticks++ > 40) {
                    this.cancel();
                    return;
                }
                double angleStep = 0.6283185307179586;
                for (double angle = 0.0; angle < Math.PI * 2; angle += angleStep) {
                    double x = Math.cos(angle) * 0.85;
                    double z = Math.sin(angle) * 0.85;
                    double yOffset = (double) this.ticks * 0.07;
                    Location loc = base.clone().add(x, yOffset, z);
                    if (loc.getY() > center.getY() + 1.8) continue;
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 0);
                    Color color = switch (mat) {
                        case IRON_BLOCK -> Color.fromRGB(200, 200, 200);
                        case GOLD_BLOCK -> Color.fromRGB(255, 215, 0);
                        case DIAMOND_BLOCK -> Color.fromRGB(85, 255, 255);
                        case NETHERITE_BLOCK -> Color.fromRGB(60, 60, 60);
                        default -> Color.WHITE;
                    };
                    world.spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(color, 1.4f));
                }
            }
        }.runTaskTimer(Bastion.getInstance(), 0L, 2L);
    }

    private void showBossBar(Player player) {
        String title = Utils.color(this.config.getString("settings.bossbar.title"));
        BossBar bar = Bukkit.createBossBar((String)title, (BarColor)BarColor.RED, (BarStyle)BarStyle.SOLID, (BarFlag[])new BarFlag[0]);
        bar.addPlayer(player);
        bar.setProgress(1.0);
        this.bossBars.put(player.getUniqueId(), bar);
    }

    private void removeBossBar(Player player) {
        BossBar bar = this.bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removeAll();
        }
    }
}
