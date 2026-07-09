package dev.portfolio.bastion.Clans;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import dev.portfolio.bastion.Bastion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class DataManager {
    private final Bastion plugin;
    private File file;
    private FileConfiguration config;

    public DataManager(Bastion plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        this.file = new File(this.plugin.getDataFolder(), "data.yml");
        if (!this.file.exists()) {
            this.plugin.saveResource("data.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration((File)this.file);
        if (this.config.getConfigurationSection("clans") == null) {
            this.config.createSection("clans");
            this.save();
        }
    }

    public void save() {
        try {
            this.config.save(this.file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return this.config;
    }

    public boolean isInClan(Player player) {
        return this.getClan(player) != null;
    }

    public String getClan(Player player) {
        if (this.config.getConfigurationSection("clans") == null) {
            return null;
        }
        for (String clan : this.config.getConfigurationSection("clans").getKeys(false)) {
            List members = this.config.getStringList("clans." + clan + ".members");
            if (members.contains(player.getUniqueId().toString())) {
                return clan;
            }
            String owner = this.config.getString("clans." + clan + ".owner");
            if (owner == null || !owner.equals(player.getUniqueId().toString())) continue;
            return clan;
        }
        return null;
    }

    public List<String> getMembers(String clan) {
        return this.config.getStringList("clans." + clan + ".members");
    }

    public String getOwner(String clan) {
        return this.config.getString("clans." + clan + ".owner");
    }

    public int getBases(String clan) {
        return this.config.getInt("clans." + clan + ".bases", 1);
    }

    public void setBases(String clan, int amount) {
        this.config.set("clans." + clan + ".bases", (Object)amount);
        this.save();
    }

    public List<String> getPotions(String clan) {
        return this.config.getStringList("clans." + clan + ".potions");
    }

    public void addPotion(String clan, String potion) {
        List<String> list = this.getPotions(clan);
        if (!list.contains(potion)) {
            list.add(potion);
            this.config.set("clans." + clan + ".potions", list);
            this.save();
        }
    }

    public String getClanDisplay(String clan) {
        return this.config.getString("clans." + clan + ".display", clan);
    }

    public void setBase(String clan, String name, Location loc) {
        String path = "clans." + clan + ".basesData." + name;
        this.config.set(path + ".world", (Object)loc.getWorld().getName());
        this.config.set(path + ".x", (Object)loc.getX());
        this.config.set(path + ".y", (Object)loc.getY());
        this.config.set(path + ".z", (Object)loc.getZ());
        this.config.set(path + ".yaw", (Object)Float.valueOf(loc.getYaw()));
        this.config.set(path + ".pitch", (Object)Float.valueOf(loc.getPitch()));
        this.save();
    }

    public Location getBase(String clan, String name) {
        String path = "clans." + clan + ".basesData." + name;
        if (!this.config.contains(path)) {
            return null;
        }
        World world = Bukkit.getWorld(this.config.getString(path + ".world"));
        if (world == null) {
            return null;
        }
        return new Location(world, this.config.getDouble(path + ".x"), this.config.getDouble(path + ".y"), this.config.getDouble(path + ".z"), (float)this.config.getDouble(path + ".yaw"), (float)this.config.getDouble(path + ".pitch"));
    }

    public void removeBase(String clan, String name) {
        this.config.set("clans." + clan + ".basesData." + name, null);
        this.save();
    }

    public Set<String> getBasesList(String clan) {
        String path = "clans." + clan + ".basesData";
        if (this.config.getConfigurationSection(path) == null) {
            return new HashSet<String>();
        }
        return this.config.getConfigurationSection(path).getKeys(false);
    }

    public Location getBaseByIndex(String clan, int index) {
        String path = "clans." + clan + ".basesData";
        ConfigurationSection sec = this.config.getConfigurationSection(path);
        if (sec == null) {
            return null;
        }
        ArrayList keys = new ArrayList(sec.getKeys(false));
        if (index < 1 || index > keys.size()) {
            return null;
        }
        String key = (String)keys.get(index - 1);
        return this.getBase(clan, key);
    }

    public String getBaseNameByIndex(String clan, int index) {
        String path = "clans." + clan + ".basesData";
        ConfigurationSection sec = this.config.getConfigurationSection(path);
        if (sec == null) {
            return "";
        }
        ArrayList keys = new ArrayList(sec.getKeys(false));
        if (index < 1 || index > keys.size()) {
            return "";
        }
        return (String)keys.get(index - 1);
    }

    public boolean isClanPvpEnabled(String clan) {
        String path = "clans." + clan + ".pvp";
        if (!this.config.contains(path)) {
            this.config.set(path, (Object)true);
            this.save();
            return true;
        }
        return this.config.getBoolean(path);
    }

    public void setClanPvp(String clan, boolean enabled) {
        this.config.set("clans." + clan + ".pvp", (Object)enabled);
        this.save();
    }

    public boolean toggleClanPvp(String clan) {
        boolean newState = !this.isClanPvpEnabled(clan);
        this.setClanPvp(clan, newState);
        return newState;
    }
}
