package dev.portfolio.bastion.Privates;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import dev.portfolio.bastion.Privates.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class RegionService {
    private final ConfigManager config;

    public RegionService(ConfigManager config) {
        this.config = config;
    }

    public RegionManager getManager(World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt((World)world));
    }

    public String generateId(Location loc) {
        return "ps_x" + loc.getBlockX() + "y" + loc.getBlockY() + "z" + loc.getBlockZ();
    }

    public boolean hasLimit(Player p) {
        RegionManager manager = this.getManager(p.getWorld());
        if (manager == null) {
            return false;
        }
        long count = manager.getRegions().values().stream()
                .filter(r -> r.getId().startsWith("ps_x"))
                .filter(r -> r.isOwner(WorldGuardPlugin.inst().wrapPlayer(p)))
                .count();
        return count < (long) this.config.getLimit(p);
    }

    public boolean canCreate(Player p, Location loc, int radius) {
        RegionManager manager = this.getManager(loc.getWorld());
        if (manager == null) {
            return false;
        }
        BlockVector3 min = BlockVector3.at(loc.getBlockX() - radius, loc.getBlockY() - radius, loc.getBlockZ() - radius);
        BlockVector3 max = BlockVector3.at(loc.getBlockX() + radius, loc.getBlockY() + radius, loc.getBlockZ() + radius);

        ProtectedCuboidRegion temp = new ProtectedCuboidRegion("temp", min, max);
        ApplicableRegionSet set = manager.getApplicableRegions(temp);
        for (ProtectedRegion r : set) {
            if (r.isOwner(WorldGuardPlugin.inst().wrapPlayer(p))) continue;
            return false;
        }
        return true;
    }

    public void createRegion(Player p, Location loc, int radius, String id) {
        RegionManager manager = this.getManager(loc.getWorld());
        if (manager == null) {
            return;
        }
        BlockVector3 min = BlockVector3.at(loc.getBlockX() - radius, loc.getBlockY() - radius, loc.getBlockZ() - radius);
        BlockVector3 max = BlockVector3.at(loc.getBlockX() + radius, loc.getBlockY() + radius, loc.getBlockZ() + radius);
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(id, min, max);
        region.getOwners().addPlayer(p.getUniqueId());
        List<String> allow = this.config.getStringList("settings.allow-flags");
        List<String> deny = this.config.getStringList("settings.deny-flags");
        this.applyFlags(region, allow, deny);
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        Flag raw = registry.get("deny-message");
        if (raw instanceof StringFlag flag) {
            region.setFlag(flag, "");
        }
        manager.addRegion(region);
        try {
            manager.save();
        } catch (StorageException ex) {
            ex.printStackTrace();
        }
    }

    public ProtectedRegion getRegionByBlock(Location loc) {
        if (loc.getWorld() == null) {
            return null;
        }
        String id = this.generateId(loc);
        RegionManager manager = this.getManager(loc.getWorld());
        if (manager == null) {
            return null;
        }
        return manager.getRegion(id);
    }

    public List<ProtectedRegion> getPlayerPrivateRegions(Player p) {
        RegionManager manager = this.getManager(p.getWorld());
        if (manager == null) {
            return Collections.emptyList();
        }
        return manager.getApplicableRegions(BukkitAdapter.asBlockVector(p.getLocation())).getRegions().stream()
                .filter(r -> r.getId().startsWith("ps_x"))
                .toList();
    }

    public BlockVector3 getCenter(ProtectedRegion region) {
        return region.getMinimumPoint().add(region.getMaximumPoint()).divide(2);
    }

    public Material getRegionMaterial(ProtectedRegion region, World world) {
        BlockVector3 center = this.getCenter(region);
        return world.getBlockAt(center.x(), center.y(), center.z()).getType();
    }

    public ProtectedRegion getRegionAt(Location loc) {
        RegionManager manager = this.getManager(loc.getWorld());
        if (manager == null) {
            return null;
        }
        ApplicableRegionSet set = manager.getApplicableRegions(BlockVector3.at((int)loc.getBlockX(), (int)loc.getBlockY(), (int)loc.getBlockZ()));
        return set.getRegions().stream().filter(r -> r.getId().startsWith("ps_x")).findFirst().orElse(null);
    }

    public void applyFlags(ProtectedRegion region, List<String> allow, List<String> deny) {
        Flag flag;
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        for (String name : allow) {
            flag = registry.get(name.toLowerCase(Locale.ROOT));
            if (flag == null) continue;
            region.setFlag(flag, this.getAllowValue(flag));
        }
        for (String name : deny) {
            flag = registry.get(name.toLowerCase(Locale.ROOT));
            if (flag == null) continue;
            region.setFlag(flag, this.getDenyValue(flag));
        }
    }

    private Object getAllowValue(Flag<?> flag) {
        if (flag instanceof StateFlag) {
            return StateFlag.State.ALLOW;
        }
        return true;
    }

    private Object getDenyValue(Flag<?> flag) {
        if (flag instanceof StateFlag) {
            return StateFlag.State.DENY;
        }
        return false;
    }
}
