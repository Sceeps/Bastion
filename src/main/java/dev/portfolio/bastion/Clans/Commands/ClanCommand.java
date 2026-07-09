package dev.portfolio.bastion.Clans.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import dev.portfolio.bastion.Clans.ClanGlow.ClanGlowManager;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Clans.GUIManager;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClanCommand
implements TabExecutor {
    private final DataManager data;
    private final GUIManager gui;
    private final ClanGlowManager glowManager;

    private final Map<UUID, Long> deleteConfirm = new HashMap<UUID, Long>();

    private final Map<UUID, String> invites = new HashMap<UUID, String>();

    private final Pattern pattern = Pattern.compile(Bastion.getInstance().getConfig().getString("clanSettings.allowedSymbols", "[0-9а-яА-Яa-zA-Z_&-]*"));

    public ClanCommand(DataManager data, GUIManager gui, ClanGlowManager glowManager) {
        this.data = data;
        this.gui = gui;
        this.glowManager = glowManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player)sender;
        if (args.length == 0) {
            this.gui.openMainMenu(player);
            return true;
        }
        FileConfiguration config = Bastion.getInstance().getConfig();
        switch (args[0].toLowerCase()) {
            case "create": {
                if (this.data.isInClan(player)) {
                    player.sendMessage(Utils.color(config.getString("messages.alreadyInClan"), (OfflinePlayer)player));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                String rawName = args[1];
                String coloredName = Utils.color(rawName);
                String cleanName = ChatColor.stripColor((String)coloredName);
                if (this.data.getConfig().contains("clans." + cleanName)) {
                    player.sendMessage(Utils.color(config.getString("messages.clanAlreadyExists"), (OfflinePlayer)player));
                    return true;
                }
                int min = Bastion.getInstance().getConfig().getInt("clanSettings.minSymbols");
                int max = Bastion.getInstance().getConfig().getInt("clanSettings.maxSymbols");
                if (cleanName.length() < min) {
                    player.sendMessage(Utils.color(config.getString("messages.minLimit"), (OfflinePlayer)player));
                    return true;
                }
                if (cleanName.length() > max) {
                    player.sendMessage(Utils.color(config.getString("messages.maxLimit"), (OfflinePlayer)player));
                    return true;
                }
                if (!this.pattern.matcher(rawName).matches()) {
                    player.sendMessage(Utils.color(config.getString("messages.badSymbols"), (OfflinePlayer)player));
                    return true;
                }
                int price = Bastion.getInstance().getConfig().getInt("clanSettings.createPrice");
                if (!Bastion.getEcon().has((OfflinePlayer)player, (double)price)) {
                    player.sendMessage(Utils.color(config.getString("messages.noMoney"), (OfflinePlayer)player));
                    return true;
                }
                Bastion.getEcon().withdrawPlayer((OfflinePlayer)player, (double)price);
                this.data.getConfig().set("clans." + cleanName + ".owner", (Object)player.getUniqueId().toString());
                this.data.getConfig().set("clans." + cleanName + ".members", new ArrayList());
                this.data.getConfig().set("clans." + cleanName + ".bases", (Object)1);
                this.data.getConfig().set("clans." + cleanName + ".potions", new ArrayList());
                this.data.getConfig().set("clans." + cleanName + ".display", (Object)coloredName);
                this.data.getConfig().set("clans." + cleanName + ".pvp", (Object)false);
                this.data.save();
                player.sendMessage(Utils.color(config.getString("messages.createClan").replace("{clan}", coloredName), (OfflinePlayer)player));
                break;
            }
            case "invite": {
                if (!this.data.isInClan(player)) {
                    player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                String clan = this.data.getClan(player);
                if (!this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
                    player.sendMessage(Utils.color(config.getString("messages.onlyOwnerCanInvite"), (OfflinePlayer)player));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    player.sendMessage(Utils.color(config.getString("messages.noPlayer"), (OfflinePlayer)player));
                    return true;
                }
                if (this.data.isInClan(target)) {
                    player.sendMessage(Utils.color(config.getString("messages.alreadyInClan"), (OfflinePlayer)player));
                    return true;
                }
                int maxInvite = Bastion.getInstance().getConfig().getInt("clanSettings.maxMembers");

                if (this.data.getMembers(clan).size() + 1 >= maxInvite) {
                    player.sendMessage(Utils.color(config.getString("messages.clanFilled"), (OfflinePlayer)player));
                    return true;
                }
                this.invites.put(target.getUniqueId(), clan);
                player.sendMessage(Utils.color(config.getString("messages.inviteSend").replace("{target}", target.getName()), (OfflinePlayer)player));
                target.sendMessage(Utils.color(config.getString("messages.inviteReceive").replace("{cleanName}", clan).replace("{clan}", this.data.getClanDisplay(clan)), (OfflinePlayer)target));
                break;
            }
            case "join": {
                if (this.data.isInClan(player)) {
                    player.sendMessage(Utils.color(config.getString("messages.alreadyInClan"), (OfflinePlayer)player));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                String clan = args[1];
                if (!this.invites.containsKey(player.getUniqueId()) || !this.invites.get(player.getUniqueId()).equals(clan)) {
                    player.sendMessage(Utils.color(config.getString("messages.joinEmpty"), (OfflinePlayer)player));
                    return true;
                }
                if (!this.data.getConfig().contains("clans." + clan + ".owner")) {
                    this.invites.remove(player.getUniqueId());
                    player.sendMessage(Utils.color(config.getString("messages.joinEmpty"), (OfflinePlayer)player));
                    return true;
                }
                int max = Bastion.getInstance().getConfig().getInt("clanSettings.maxMembers");
                if (this.data.getMembers(clan).size() + 1 >= max) {
                    player.sendMessage(Utils.color(config.getString("messages.clanFilled"), (OfflinePlayer)player));
                    return true;
                }
                List<String> members = this.data.getMembers(clan);
                members.add(player.getUniqueId().toString());
                this.data.getConfig().set("clans." + clan + ".members", members);
                this.data.save();
                this.invites.remove(player.getUniqueId());
                player.sendMessage(Utils.color(config.getString("messages.clanJoin").replace("{clan}", this.data.getClanDisplay(clan)), (OfflinePlayer)player));
                this.sendClanMessage(clan, config.getString("messages.clanMsgJoin").replace("{player}", player.getName()), player.getUniqueId());
                break;
            }
            case "leave": {
                String clan = this.data.getClan(player);
                if (clan == null) {
                    player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
                    return true;
                }
                if (this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
                    player.sendMessage(Utils.color(config.getString("messages.cantLeave"), (OfflinePlayer)player));
                    return true;
                }
                List<String> members = this.data.getMembers(clan);
                members.remove(player.getUniqueId().toString());
                this.data.getConfig().set("clans." + clan + ".members", members);
                this.data.save();
                player.sendMessage(Utils.color(config.getString("messages.clanLeave"), (OfflinePlayer)player));
                this.sendClanMessage(clan, config.getString("messages.clanMsgLeave").replace("{player}", player.getName()), player.getUniqueId());
                break;
            }
            case "kick": {
                String clan = this.data.getClan(player);
                if (clan == null) {
                    player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                if (!this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
                    player.sendMessage(Utils.color(config.getString("messages.onlyOwnerCanKick"), (OfflinePlayer)player));
                    return true;
                }
                OfflinePlayer targetOff = Bukkit.getOfflinePlayer(args[1]);
                if (targetOff.getUniqueId() == null || (!targetOff.hasPlayedBefore() && !targetOff.isOnline())) {
                    player.sendMessage(Utils.color(config.getString("messages.noPlayer"), (OfflinePlayer)player));
                    return true;
                }
                String targetId = targetOff.getUniqueId().toString();
                List<String> membersKick = this.data.getMembers(clan);
                if (!membersKick.contains(targetId)) {
                    player.sendMessage(Utils.color(config.getString("messages.unknownPlayer"), (OfflinePlayer)player));
                    return true;
                }
                Player targetOnline = targetOff.getPlayer();
                if (targetOnline != null && Bastion.getManager().isTarget(targetOnline)) {
                    player.sendMessage(Utils.color(config.getString("messages.cannotKickClanTarget"), (OfflinePlayer)player));
                    return true;
                }
                membersKick.remove(targetId);
                this.data.getConfig().set("clans." + clan + ".members", membersKick);
                this.data.save();
                if (targetOnline != null) {
                    targetOnline.sendMessage(Utils.color(config.getString("messages.clanKick"), (OfflinePlayer)targetOnline));
                }
                String kickName = targetOff.getName() != null ? targetOff.getName() : args[1];
                this.sendClanMessage(clan, config.getString("messages.clanMsgKick").replace("{player}", kickName), targetOff.getUniqueId());
                break;
            }
            case "disband": {
                long time;
                String clan = this.data.getClan(player);
                if (clan == null) {
                    player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
                    return true;
                }
                if (!this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
                    player.sendMessage(Utils.color(config.getString("messages.onlyOwnerCanDisband"), (OfflinePlayer)player));
                    return true;
                }
                long now = System.currentTimeMillis();
                if (this.deleteConfirm.containsKey(player.getUniqueId()) && now - (time = this.deleteConfirm.get(player.getUniqueId()).longValue()) <= 10000L) {
                    this.sendClanMessage(clan, config.getString("messages.clanMsgDisband"), player.getUniqueId());
                    player.sendMessage(Utils.color(config.getString("messages.clanMsgDisband"), (OfflinePlayer)player));
                    this.data.getConfig().set("clans." + clan, null);
                    this.data.save();
                    this.deleteConfirm.remove(player.getUniqueId());
                    this.invites.entrySet().removeIf(e -> clan.equals(e.getValue()));
                    this.glowManager.disableClan(clan);
                    return true;
                }
                this.deleteConfirm.put(player.getUniqueId(), now);
                player.sendMessage(Utils.color(config.getString("messages.clanDisbandConfirm"), (OfflinePlayer)player));
                break;
            }
            case "name": {
                String clan = this.data.getClan(player);
                if (clan == null) {
                    player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
                    return true;
                }
                if (!this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
                    player.sendMessage(Utils.color(config.getString("messages.onlyOwnerCanName"), (OfflinePlayer)player));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                String rawName = args[1];
                String coloredName = Utils.color(rawName);
                String cleanName = ChatColor.stripColor(coloredName);
                int min = Bastion.getInstance().getConfig().getInt("clanSettings.minSymbols");
                int maxSym = Bastion.getInstance().getConfig().getInt("clanSettings.maxSymbols");
                if (cleanName.length() < min) {
                    player.sendMessage(Utils.color(config.getString("messages.minLimit"), (OfflinePlayer)player));
                    return true;
                }
                if (cleanName.length() > maxSym) {
                    player.sendMessage(Utils.color(config.getString("messages.maxLimit"), (OfflinePlayer)player));
                    return true;
                }
                if (!this.pattern.matcher(rawName).matches()) {
                    player.sendMessage(Utils.color(config.getString("messages.badSymbols"), (OfflinePlayer)player));
                    return true;
                }
                if (this.data.getConfig().contains("clans." + cleanName)) {
                    player.sendMessage(Utils.color(config.getString("messages.clanAlreadyExists"), (OfflinePlayer)player));
                    return true;
                }
                ConfigurationSection oldSection = this.data.getConfig().getConfigurationSection("clans." + clan);
                if (oldSection == null) {
                    return true;
                }
                ConfigurationSection newSection = this.data.getConfig().createSection("clans." + cleanName);
                this.copySection(oldSection, newSection);
                newSection.set("display", coloredName);
                this.data.getConfig().set("clans." + clan, null);
                this.data.save();
                this.invites.replaceAll((uuid, invClan) -> clan.equals(invClan) ? cleanName : invClan);
                this.glowManager.renameClan(clan, cleanName);
                this.sendClanMessage(cleanName, config.getString("messages.clanMsgName").replace("{clan}", coloredName), player.getUniqueId());
                player.sendMessage(Utils.color(config.getString("messages.clanName").replace("{clan}", coloredName), (OfflinePlayer)player));
                break;
            }
            case "msg": {
                String clan = this.data.getClan(player);
                if (clan == null) {
                    player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
                    return true;
                }
                if (args.length < 2) {
                    return true;
                }
                String message = String.join((CharSequence)" ", Arrays.copyOfRange(args, 1, args.length));
                String format = Bastion.getInstance().getConfig().getString("clanSettings.clanMsgFormat");
                format = format.replace("{clan}", clan).replace("{player}", player.getName()).replace("{message}", message);
                for (String uuid : this.data.getMembers(clan)) {
                    Player p = Bukkit.getPlayer((UUID)UUID.fromString(uuid));
                    if (p == null) continue;
                    p.sendMessage(Utils.color(format));
                }
                Player owner = Bukkit.getPlayer((UUID)UUID.fromString(this.data.getOwner(clan)));
                if (owner == null) break;
                owner.sendMessage(Utils.color(format));
                break;
            }
            case "pvp": {
                String clan = this.data.getClan(player);
                if (clan == null) {
                    player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
                    return true;
                }
                if (!this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
                    player.sendMessage(Utils.color(config.getString("messages.onlyOwnerCanPvpToggle"), (OfflinePlayer)player));
                    return true;
                }
                boolean enabled = this.data.toggleClanPvp(clan);
                if (enabled) {
                    this.sendClanMessage(clan, Utils.color(config.getString("messages.clanPvpEnable"), (OfflinePlayer)player), null);
                } else {
                    this.sendClanMessage(clan, Utils.color(config.getString("messages.clanPvpDisable"), (OfflinePlayer)player), null);
                }
                return true;
            }
            case "reload": {
                if (!player.hasPermission("bastion.reload")) {
                    player.sendMessage(Utils.color(config.getString("messages.noPerm"), (OfflinePlayer)player));
                    return true;
                }
                Bastion.getInstance().reload();
                player.sendMessage(Utils.color(config.getString("messages.reload"), (OfflinePlayer)player));
                return true;
            }
            default: {
                this.gui.openMainMenu(player);
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
            if (!this.data.isInClan(player)) {
                sub.addAll(List.of("create", "join"));
            } else {
                sub.addAll(List.of("invite", "leave", "msg"));
                String clan = this.data.getClan(player);
                if (this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
                    sub.addAll(List.of("disband", "kick", "name", "pvp"));
                }
            }
            if (player.hasPermission("bastion.reload")) {
                sub.add("reload");
            }
            return this.filter(sub, args[0]);
        }
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "invite":
                case "kick": {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (args[0].equalsIgnoreCase("invite") && this.data.isInClan(online) || args[0].equalsIgnoreCase("kick") && (!this.data.isInClan(player) || !this.data.getClan(player).equals(this.data.getClan(online)) || online.equals((Object)player))) continue;
                        result.add(online.getName());
                    }
                    break;
                }
                case "join": {
                    if (this.data.getConfig().getConfigurationSection("clans") == null) {
                        return List.of();
                    }
                    result.addAll(this.data.getConfig().getConfigurationSection("clans").getKeys(false));
                    break;
                }
                case "name": {
                    result.add("новое_имя");
                }
            }
            return this.filter(result, args[1]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("msg")) {
            return List.of();
        }
        return List.of();
    }

    private List<String> filter(List<String> list, String arg) {
        ArrayList<String> result = new ArrayList<String>();
        for (String s : list) {
            if (!s.toLowerCase().startsWith(arg.toLowerCase())) continue;
            result.add(s);
        }
        return result;
    }

    private void sendClanMessage(String clan, String message, UUID exclude) {
        Player p;
        for (String uuidStr : this.data.getMembers(clan)) {
            if (uuidStr == null || uuidStr.isEmpty()) continue;
            try {
                Player p2;
                UUID uuid = UUID.fromString(uuidStr);
                if (uuid.equals(exclude) || (p2 = Bukkit.getPlayer((UUID)uuid)) == null) continue;
                p2.sendMessage(Utils.color(message, (OfflinePlayer)p2));
            }
            catch (Exception exception) {}
        }
        UUID owner = UUID.fromString(this.data.getOwner(clan));
        if (!owner.equals(exclude) && (p = Bukkit.getPlayer((UUID)owner)) != null) {
            p.sendMessage(Utils.color(message, (OfflinePlayer)p));
        }
    }

    private void copySection(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection) {
                ConfigurationSection section = (ConfigurationSection)value;
                ConfigurationSection newSection = to.createSection(key);
                this.copySection(section, newSection);
                continue;
            }
            to.set(key, value);
        }
    }
}
