package dev.portfolio.bastion.Clans.Commands;

import java.util.List;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SetbaseCommand
implements TabExecutor {
    private final DataManager data;

    public SetbaseCommand(DataManager data) {
        this.data = data;
    }

    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) {
            return true;
        }
        Player player = (Player)commandSender;
        FileConfiguration config = Bastion.getInstance().getConfig();
        String clan = this.data.getClan(player);
        if (clan == null) {
            player.sendMessage(Utils.color(config.getString("messages.noClan"), (OfflinePlayer)player));
            return true;
        }
        if (!this.data.getOwner(clan).equals(player.getUniqueId().toString())) {
            player.sendMessage(Utils.color(config.getString("messages.onlyOwnerCanInvite"), (OfflinePlayer)player));
            return true;
        }
        if (strings.length < 1) {
            return true;
        }
        List blacklistWorlds = config.getStringList("clanSettings.blacklistWorlds");
        if (blacklistWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(Utils.color(config.getString("messages.blacklistWorld"), (OfflinePlayer)player));
            return true;
        }
        String name = strings[0];
        if (!name.matches(config.getString("clanSettings.allowedSymbolsForBases", "[0-9а-яА-Яa-zA-Z_-]*"))) {
            player.sendMessage(Utils.color(config.getString("messages.invalidBaseName"), (OfflinePlayer)player));
            return true;
        }
        int max = this.data.getBases(clan);
        if (this.data.getBasesList(clan).size() >= max) {
            player.sendMessage(Utils.color(config.getString("messages.maxBases"), (OfflinePlayer)player));
            return true;
        }
        this.data.setBase(clan, name, player.getLocation());
        player.sendMessage(Utils.color(config.getString("messages.clanSetBase"), (OfflinePlayer)player));
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of();
    }
}
