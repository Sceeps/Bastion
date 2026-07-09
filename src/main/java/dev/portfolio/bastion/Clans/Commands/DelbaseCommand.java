package dev.portfolio.bastion.Clans.Commands;

import java.util.ArrayList;
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

public class DelbaseCommand
implements TabExecutor {
    private final DataManager data;

    public DelbaseCommand(DataManager data) {
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
        String name = strings[0];
        this.data.removeBase(clan, name);
        player.sendMessage(Utils.color(config.getString("messages.clanRemoveBase"), (OfflinePlayer)player));
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (!(commandSender instanceof Player)) {
            return List.of();
        }
        Player player = (Player)commandSender;
        if (strings.length == 1) {
            ArrayList<String> result = new ArrayList<String>();
            String clan = this.data.getClan(player);
            if (clan == null) {
                return List.of();
            }
            result.addAll(this.data.getBasesList(clan));
            return result;
        }
        return List.of();
    }
}
