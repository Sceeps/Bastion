package dev.portfolio.bastion.Clans;

import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ClansPlaceholder
extends PlaceholderExpansion {
    private final DataManager data;
    private final Bastion plugin;

    public ClansPlaceholder(DataManager data, Bastion plugin) {
        this.data = data;
        this.plugin = plugin;
    }

    @NotNull
    public String getIdentifier() {
        return "cap";
    }

    @NotNull
    public String getAuthor() {
        return "";
    }

    @NotNull
    public String getVersion() {
        return "";
    }

    public boolean persist() {
        return true;
    }

    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("clan")) {
            if (player == null) {
                return " ";
            }
            String clan = this.data.getClan(player);
            if (clan == null) {
                return Utils.color(this.plugin.getConfig().getString("clanSettings.placeholderNoClan"));
            }
            return this.data.getClanDisplay(clan);
        }
        return null;
    }
}
