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

public class SwitchCommand
implements TabExecutor {
    private final GreatHuntManager manager;

    public SwitchCommand(GreatHuntManager manager) {
        this.manager = manager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("greathunt.switch")) {
            sender.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.noPerm")));
            return true;
        }
        boolean current = Bastion.getInstance().getConfig().getBoolean("huntSettings.enable");
        boolean newValue = !current;
        Bastion.getInstance().getConfig().set("huntSettings.enable", (Object)newValue);
        Bastion.getInstance().saveConfig();
        if (newValue) {
            sender.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.enable")));
            this.manager.startScheduler();
        } else {
            sender.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.disable")));
            this.manager.stopAll();
        }
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}
