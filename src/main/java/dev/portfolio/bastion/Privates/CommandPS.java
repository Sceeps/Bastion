package dev.portfolio.bastion.Privates;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import dev.portfolio.bastion.Privates.ConfigManager;
import dev.portfolio.bastion.Privates.HologramService;
import dev.portfolio.bastion.Privates.RegionService;
import dev.portfolio.bastion.Privates.ToggleManager;
import dev.portfolio.bastion.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CommandPS
implements TabExecutor {
    private final ToggleManager toggle;
    private final RegionService regionService;
    private final HologramService hologramService;
    private final ConfigManager config;

    public CommandPS(ToggleManager toggle, RegionService regionService, HologramService hologramService, ConfigManager config) {
        this.toggle = toggle;
        this.regionService = regionService;
        this.hologramService = hologramService;
        this.config = config;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            this.sendHelp(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "toggle": {
                this.toggle.toggle(player);
                player.sendMessage(Utils.color(this.toggle.isEnabled(player) ? this.config.getString("messages.psToggleOn") : this.config.getString("messages.psToggleOff"), (OfflinePlayer)player));
                break;
            }
            case "list": {
                RegionManager manager = this.regionService.getManager(player.getWorld());
                LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);
                List<ProtectedRegion> regions = manager.getRegions().values().stream().filter(r -> r.getId().startsWith("ps_x")).filter(r -> r.isOwner(localPlayer)).toList();
                if (regions.isEmpty()) {
                    player.sendMessage(Utils.color(this.config.getString("messages.noPrivateList"), (OfflinePlayer)player));
                    return true;
                }
                for (String string : this.config.getStringList("messages.list.header")) {
                    player.sendMessage(Utils.color(string, (OfflinePlayer)player));
                }
                String format = this.config.getString("messages.list.format");
                for (ProtectedRegion r2 : regions) {
                    int members = r2.getMembers().getUniqueIds().size();
                    int owners = r2.getOwners().size();
                    int total = members + owners;
                    String msg = format.replace("{id}", r2.getId()).replace("{members}", String.valueOf(total));
                    player.sendMessage(Utils.color(msg, (OfflinePlayer)player));
                }
                for (String line : this.config.getStringList("messages.list.footer")) {
                    player.sendMessage(Utils.color(line, (OfflinePlayer)player));
                }
                break;
            }
            case "add": {
                if (args.length < 2) {
                    this.sendHelp(player);
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) {
                    player.sendMessage(Utils.color(this.config.getString("messages.noPlayer"), (OfflinePlayer)player));
                    return true;
                }
                List<ProtectedRegion> regions = this.regionService.getPlayerPrivateRegions(player);
                if (regions.isEmpty()) {
                    player.sendMessage(Utils.color(this.config.getString("messages.noPrivate"), (OfflinePlayer)player));
                    return true;
                }
                LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(player);
                UUID targetUUID = target.getUniqueId();
                if (targetUUID.equals(player.getUniqueId())) {
                    player.sendMessage(Utils.color(this.config.getString("messages.psAddMirror"), (OfflinePlayer)player));
                    return true;
                }
                boolean alreadyMember = false;
                boolean changed = false;
                for (ProtectedRegion region : regions) {
                    if (!region.isOwner(lp)) continue;
                    if (region.getMembers().contains(targetUUID)) {
                        alreadyMember = true;
                        continue;
                    }
                    region.getMembers().addPlayer(targetUUID);
                    this.hologramService.update(region.getId(), region, player);
                    changed = true;
                }
                if (alreadyMember && !changed) {
                    player.sendMessage(Utils.color(this.config.getString("messages.psAddAlreadyMember"), (OfflinePlayer)player));
                }
                if (!changed) break;
                try {
                    var mgr = this.regionService.getManager(player.getWorld());
                    if (mgr != null) mgr.save();
                } catch (StorageException e) {
                    throw new RuntimeException(e);
                }
                player.sendMessage(Utils.color(this.config.getString("messages.psAddToOwner").replace("{target}", target.getName()), (OfflinePlayer)player));
                target.sendMessage(Utils.color(this.config.getString("messages.psAddToReceiver").replace("{player}", player.getName()), (OfflinePlayer)target));
                break;
            }
            case "remove": {
                if (args.length < 2) {
                    this.sendHelp(player);
                    return true;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                List<ProtectedRegion> regions = this.regionService.getPlayerPrivateRegions(player);
                if (regions.isEmpty()) {
                    player.sendMessage(Utils.color(this.config.getString("messages.noPrivate"), (OfflinePlayer)player));
                    return true;
                }
                LocalPlayer lp = WorldGuardPlugin.inst().wrapPlayer(player);
                UUID targetUUID = target.getUniqueId();
                boolean notMember = false;
                boolean changed = false;
                for (ProtectedRegion region : regions) {
                    if (!region.isOwner(lp)) continue;
                    if (region.getOwners().contains(targetUUID)) {
                        player.sendMessage(Utils.color(this.config.getString("messages.psRemoveMirror"), (OfflinePlayer)player));
                        return true;
                    }
                    if (!region.getMembers().contains(targetUUID)) {
                        notMember = true;
                        continue;
                    }
                    region.getMembers().removePlayer(targetUUID);
                    this.hologramService.update(region.getId(), region, player);
                    changed = true;
                }
                if (notMember && !changed) {
                    player.sendMessage(Utils.color(this.config.getString("messages.notMember"), (OfflinePlayer)player));
                }
                if (!changed) break;
                try {
                    var mgr = this.regionService.getManager(player.getWorld());
                    if (mgr != null) mgr.save();
                } catch (StorageException e) {
                    throw new RuntimeException(e);
                }
                player.sendMessage(Utils.color(this.config.getString("messages.psRemoveToOwner").replace("{target}", args[1]), (OfflinePlayer)player));
                break;
            }
            case "reload": {
                if (!player.hasPermission("bastion.reload")) {
                    player.sendMessage(Utils.color(this.config.getString("messages.noPerm"), (OfflinePlayer)player));
                    return true;
                }
                this.config.load();
                player.sendMessage(Utils.color(this.config.getString("messages.reload"), (OfflinePlayer)player));
                break;
            }
            default: {
                this.sendHelp(player);
            }
        }
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return List.of();
        }
        Player player = (Player)sender;
        ArrayList<String> result = new ArrayList<String>();
        if (args.length == 1) {
            ArrayList<String> sub = new ArrayList<String>();
            sub.add("toggle");
            sub.add("list");
            sub.add("add");
            sub.add("remove");
            if (player.hasPermission("bastion.reload")) {
                sub.add("reload");
            }
            return this.filter(sub, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            UUID playerUUID = player.getUniqueId();
            for (Player online : Bukkit.getOnlinePlayers()) {
                boolean alreadyInAny;
                if (online.getUniqueId().equals(playerUUID) || (alreadyInAny = this.regionService.getPlayerPrivateRegions(player).stream().anyMatch(r -> r.getMembers().contains(online.getUniqueId())))) continue;
                result.add(online.getName());
            }
            return this.filter(result, args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            UUID playerUUID = player.getUniqueId();
            for (Player online : Bukkit.getOnlinePlayers()) {
                boolean inMember;
                if (online.getUniqueId().equals(playerUUID) || !(inMember = this.regionService.getPlayerPrivateRegions(player).stream().anyMatch(r -> r.getMembers().contains(online.getUniqueId())))) continue;
                result.add(online.getName());
            }
            return this.filter(result, args[1]);
        }
        return List.of();
    }

    private void sendHelp(Player player) {
        for (String line : this.config.getStringList("messages.help")) {
            player.sendMessage(Utils.color(line, (OfflinePlayer)player));
        }
    }

    private List<String> filter(List<String> list, String arg) {
        ArrayList<String> result = new ArrayList<String>();
        for (String s : list) {
            if (!s.toLowerCase().startsWith(arg.toLowerCase())) continue;
            result.add(s);
        }
        return result;
    }
}
