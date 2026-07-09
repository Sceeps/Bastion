package dev.portfolio.bastion.Clans;

import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Bastion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PotionTask {
    private final DataManager data;
    private final Bastion plugin;

    public PotionTask(Bastion plugin, DataManager data) {
        this.plugin = plugin;
        this.data = data;
    }

    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String clan = this.data.getClan(player);
            if (clan == null) continue;
            for (String potionId : this.data.getPotions(clan)) {
                String path = "potionsMenu.items." + potionId + ".active.effect-type";
                String raw = this.plugin.getGuiConfig().get().getString(path);
                if (raw == null) continue;
                String[] split = raw.split(";");
                PotionEffectType type = PotionEffectType.getByName((String)split[0].toUpperCase());
                int level = Integer.parseInt(split[1]) - 1;
                if (type == null) continue;
                player.addPotionEffect(new PotionEffect(type, 60, level), true);
            }
        }
    }
}
