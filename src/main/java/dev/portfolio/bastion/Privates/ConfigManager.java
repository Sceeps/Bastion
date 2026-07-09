package dev.portfolio.bastion.Privates;

import java.io.File;
import java.util.List;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final LuckPerms luckPerms;
    private File file;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.load();
    }

    public void load() {
        this.file = new File(this.plugin.getDataFolder(), "privates.yml");
        if (!this.file.exists()) {
            this.plugin.saveResource("privates.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration((File)this.file);
    }

    public int getLimit(Player player) {
        return this.getMaxPrivates(player);
    }

    public String getSound() {
        return this.config.getString("settings.set_sound");
    }

    public boolean isPrivateBlock(Material mat) {
        return this.config.contains("settings." + mat.name().toLowerCase());
    }

    public int getRadius(Material mat) {
        return this.config.getInt("settings." + mat.name().toLowerCase() + ".radius");
    }

    public List<String> getHologramLines(Material mat) {
        return this.config.getStringList("settings." + mat.name().toLowerCase() + ".hologram");
    }

    public String getString(String path) {
        return this.config.getString(path);
    }

    public List<String> getStringList(String path) {
        return this.config.getStringList(path);
    }

    public double getDouble(String path) {
        return this.config.getDouble(path);
    }

    private int getMaxPrivates(Player player) {
        User user = this.luckPerms.getPlayerAdapter(Player.class).getUser(player);
        return this.config.getInt("settings.private_count." + user.getPrimaryGroup(), 3);
    }
}
