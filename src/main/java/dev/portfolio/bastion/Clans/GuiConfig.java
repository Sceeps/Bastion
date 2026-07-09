package dev.portfolio.bastion.Clans;

import java.io.File;
import java.io.IOException;
import dev.portfolio.bastion.Bastion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class GuiConfig {
    private final Bastion plugin;
    private File file;
    private FileConfiguration config;

    public GuiConfig(Bastion plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        this.file = new File(this.plugin.getDataFolder(), "guis.yml");
        if (!this.file.exists()) {
            this.plugin.saveResource("guis.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration((File)this.file);
    }

    public void save() {
        try {
            this.config.save(this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration get() {
        return this.config;
    }
}
