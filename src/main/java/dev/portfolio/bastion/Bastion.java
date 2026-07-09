package dev.portfolio.bastion;

import dev.portfolio.bastion.Clans.ClanGlow.ClanGlowManager;
import dev.portfolio.bastion.Clans.ClansPlaceholder;
import dev.portfolio.bastion.Clans.Commands.BaseCommand;
import dev.portfolio.bastion.Clans.Commands.ClanCommand;
import dev.portfolio.bastion.Clans.Commands.DelbaseCommand;
import dev.portfolio.bastion.Clans.Commands.SetbaseCommand;
import dev.portfolio.bastion.Clans.DamageListener;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Clans.FriendlyFireListener;
import dev.portfolio.bastion.Clans.GUIManager;
import dev.portfolio.bastion.Clans.GuiConfig;
import dev.portfolio.bastion.Clans.MenuListener;
import dev.portfolio.bastion.Clans.PotionTask;
import dev.portfolio.bastion.GreatHunt.GreatHuntManager;
import dev.portfolio.bastion.GreatHunt.HuntListener;
import dev.portfolio.bastion.GreatHunt.ReloadCommand;
import dev.portfolio.bastion.GreatHunt.SwitchCommand;
import dev.portfolio.bastion.Privates.CommandPS;
import dev.portfolio.bastion.Privates.ConfigManager;
import dev.portfolio.bastion.Privates.CustomTntListener;
import dev.portfolio.bastion.Privates.CustomTntManager;
import dev.portfolio.bastion.Privates.HologramService;
import dev.portfolio.bastion.Privates.PrivateListener;
import dev.portfolio.bastion.Privates.RegionService;
import dev.portfolio.bastion.Privates.ToggleManager;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.command.CommandExecutor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class Bastion
extends JavaPlugin {
    private static Economy econ = null;
    private static Bastion instance;
    private static PlayerPointsAPI ppAPI;
    private GuiConfig guiConfig;
    private ClanGlowManager glowManager;
    private DataManager dataManager;
    private CustomTntManager customTntManager;
    private static GreatHuntManager manager;

    public void onEnable() {
        PlayerPoints pp;
        instance = this;
        this.saveDefaultConfig();

        manager = new GreatHuntManager();
        manager.startScheduler();
        this.getServer().getPluginManager().registerEvents((Listener)new HuntListener(manager), (Plugin)this);
        this.getCommand("ghreload").setExecutor((CommandExecutor)new ReloadCommand());
        this.getCommand("ghswitch").setExecutor((CommandExecutor)new SwitchCommand(manager));
        LuckPerms luckPerms = (LuckPerms)this.getServer().getServicesManager().load(LuckPerms.class);

        ConfigManager configManager = new ConfigManager(this, luckPerms);
        ToggleManager toggleManager = new ToggleManager();
        this.customTntManager = new CustomTntManager(this, configManager);
        this.dataManager = new DataManager(this);
        RegionService regionService = new RegionService(configManager);
        HologramService hologramService = new HologramService(configManager, regionService);
        this.getServer().getPluginManager().registerEvents((Listener)new PrivateListener(toggleManager, configManager, regionService, hologramService, this.dataManager), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new CustomTntListener(this, configManager, this.customTntManager, regionService, hologramService), (Plugin)this);
        this.getCommand("ps").setExecutor((CommandExecutor)new CommandPS(toggleManager, regionService, hologramService, configManager));

        this.guiConfig = new GuiConfig(this);
        GUIManager guiManager = new GUIManager(this, this.dataManager);
        this.glowManager = new ClanGlowManager(this.dataManager);
        this.glowManager.startParticleTask(this);
        LuckPerms luckperms = (LuckPerms)this.getServer().getServicesManager().load(LuckPerms.class);
        BaseCommand baseCommand = new BaseCommand(this.dataManager, luckperms);
        this.getCommand("clan").setExecutor((CommandExecutor)new ClanCommand(this.dataManager, guiManager, this.glowManager));
        this.getCommand("base").setExecutor((CommandExecutor)baseCommand);
        this.getCommand("setbase").setExecutor((CommandExecutor)new SetbaseCommand(this.dataManager));
        this.getCommand("delbase").setExecutor((CommandExecutor)new DelbaseCommand(this.dataManager));
        this.getServer().getPluginManager().registerEvents((Listener)new DamageListener(baseCommand), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new FriendlyFireListener(this.dataManager), (Plugin)this);

        new BukkitRunnable(){

            public void run() {
                new PotionTask(Bastion.getInstance(), Bastion.this.dataManager).run();
            }
        }.runTaskTimer((Plugin)this, 20L, 20L);
        this.getServer().getPluginManager().registerEvents((Listener)new MenuListener(this, guiManager, this.dataManager, this.glowManager, baseCommand), (Plugin)this);

        if (this.getServer().getPluginManager().isPluginEnabled("PlayerPoints") && (pp = (PlayerPoints)this.getServer().getPluginManager().getPlugin("PlayerPoints")) != null) {
            ppAPI = pp.getAPI();
        }

        if (!this.setupEconomy()) {
            this.getLogger().severe("Vault не найден");
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
        }
        new ClansPlaceholder(this.dataManager, this).register();
    }

    public void onDisable() {
        if (this.customTntManager != null) {
            this.customTntManager.unregisterRecipe();
        }
        manager.stopAll();
    }

    public void reload() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.dataManager.load();
        this.guiConfig.load();
    }

    public static Bastion getInstance() {
        return instance;
    }

    public static Economy getEcon() {
        return econ;
    }

    public static PlayerPointsAPI getPP() {
        return ppAPI;
    }

    public GuiConfig getGuiConfig() {
        return this.guiConfig;
    }

    public static GreatHuntManager getManager() {
        return manager;
    }

    public DataManager getDataManager() {
        return this.dataManager;
    }

    public ClanGlowManager getGlowManager() {
        return this.glowManager;
    }

    private boolean setupEconomy() {
        if (this.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = (Economy)rsp.getProvider();
        return true;
    }
}
