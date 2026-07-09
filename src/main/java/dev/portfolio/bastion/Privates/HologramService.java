package dev.portfolio.bastion.Privates;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import java.util.List;
import dev.portfolio.bastion.Privates.ConfigManager;
import dev.portfolio.bastion.Privates.RegionService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class HologramService {
    private final ConfigManager config;
    private final RegionService regionService;

    public HologramService(ConfigManager config, RegionService regionService) {
        this.config = config;
        this.regionService = regionService;
    }

    public void create(String id, Location loc, Player owner, Material mat) {
        Location holoLoc = loc.clone().add(0.5, 2.0, 0.5);
        List<String> lines = this.config.getHologramLines(mat).stream().map(l -> l.replace("{owner}", owner.getName()).replace("{members}", "1")).toList();
        DHAPI.createHologram((String)id, (Location)holoLoc, (boolean)true, lines).setDisplayRange(5);
    }

    public void update(String id, ProtectedRegion region, Player owner) {
        Hologram hologram = DHAPI.getHologram((String)id);
        if (hologram == null) {
            return;
        }
        Material mat = this.regionService.getRegionMaterial(region, owner.getWorld());
        int members = region.getMembers().getUniqueIds().size();
        int owners = region.getOwners().getUniqueIds().size();
        int total = members + owners;
        List<String> lines = this.config.getHologramLines(mat).stream().map(l -> l.replace("{owner}", owner.getName()).replace("{members}", String.valueOf(total))).toList();
        DHAPI.setHologramLines((Hologram)hologram, lines);
    }

    public void remove(String id) {
        Hologram hologram = DHAPI.getHologram((String)id);
        if (hologram == null) {
            return;
        }
        DHAPI.removeHologram((String)id);
    }
}
