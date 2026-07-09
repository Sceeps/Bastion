package dev.portfolio.bastion.GreatHunt;

import java.util.List;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.GreatHunt.GreatHuntManager;
import dev.portfolio.bastion.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ReloadCommand
implements TabExecutor {
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("greathunt.reload")) {
            sender.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.noPerm")));
            return true;
        }
        GreatHuntManager manager = Bastion.getManager();
        manager.stopAll();
        Bastion.getInstance().reloadConfig();
        manager.restartScheduler();
        sender.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.reload")));
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
