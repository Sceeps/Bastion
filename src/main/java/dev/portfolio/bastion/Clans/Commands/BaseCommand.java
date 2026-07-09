package dev.portfolio.bastion.Clans.Commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BaseCommand
implements TabExecutor {
    private final DataManager data;
    private final LuckPerms luckPerms;
    private final Map<UUID, BukkitTask> teleporting = new HashMap<UUID, BukkitTask>();
    private final Map<UUID, BossBar> bossBars = new HashMap<UUID, BossBar>();

    public BaseCommand(DataManager data, LuckPerms luckPerms) {
        this.data = data;
        this.luckPerms = luckPerms;
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
        if (strings.length < 1) {
            return true;
        }
        String name = strings[0];
        Location loc = this.data.getBase(clan, name);
        if (loc == null || loc.getWorld() == null) {
            player.sendMessage(Utils.color(config.getString("messages.unknownBase"), (OfflinePlayer)player));
            return true;
        }
        this.startTeleport(player, loc);
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

    public void startTeleport(final Player player, final Location loc) {
        if (this.isTeleporting(player)) {
            return;
        }
        FileConfiguration config = Bastion.getInstance().getConfig();
        final int duration = this.getDuration(player);
        BossBar bossBar = null;
        if (config.getBoolean("clanSettings.teleport.bossbar.enabled")) {
            bossBar = Bukkit.createBossBar((String)Utils.color(config.getString("clanSettings.teleport.bossbar.title").replace("%seconds%", String.valueOf(duration)), (OfflinePlayer)player), (BarColor)this.getColor(duration, duration), (BarStyle)BarStyle.valueOf((String)config.getString("clanSettings.teleport.bossbar.style")), (BarFlag[])new BarFlag[0]);
            bossBar.addPlayer(player);
            this.bossBars.put(player.getUniqueId(), bossBar);
        }
        player.sendMessage(Utils.color(config.getString("messages.teleportStart")).replace("%delay%", String.valueOf(duration)));
        final BossBar barRef = bossBar;
        final FileConfiguration cfg = config;
        final int totalTicks = duration * 20;
        BukkitTask task = new BukkitRunnable() {
            int ticksLeft = totalTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    BaseCommand.this.cancelTeleport(player);
                    this.cancel();
                    return;
                }
                if (this.ticksLeft <= 0) {
                    if (loc.getWorld() == null) {
                        BaseCommand.this.cancelTeleport(player);
                        this.cancel();
                        return;
                    }
                    player.teleport(loc);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    player.sendTitle(Utils.color(cfg.getString("clanSettings.teleport.title")), "", 10, 60, 10);
                    BaseCommand.this.cancelTeleport(player);
                    this.cancel();
                    return;
                }
                if (barRef != null) {
                    double progress = (double) this.ticksLeft / (double) totalTicks;
                    barRef.setProgress(Math.max(0.0, Math.min(1.0, progress)));
                    int secondsLeft = (int) Math.ceil((double) this.ticksLeft / 20.0);
                    barRef.setTitle(Utils.color(cfg.getString("clanSettings.teleport.bossbar.title")
                            .replace("%seconds%", String.valueOf(secondsLeft)), player));
                    barRef.setColor(BaseCommand.this.getColor(secondsLeft, duration));
                }
                --this.ticksLeft;
            }
        }.runTaskTimer(Bastion.getInstance(), 0L, 1L);
        this.teleporting.put(player.getUniqueId(), task);
    }

    public void cancelTeleport(Player player) {
        BossBar bossBar;
        BukkitTask task = this.teleporting.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        if ((bossBar = this.bossBars.remove(player.getUniqueId())) != null) {
            bossBar.removeAll();
        }
    }

    public boolean isTeleporting(Player player) {
        return this.teleporting.containsKey(player.getUniqueId());
    }

    private BarColor getColor(int current, int max) {
        FileConfiguration config = Bastion.getInstance().getConfig();
        double percent = (double)current / (double)max;
        if (percent > 0.6) {
            return BarColor.valueOf((String)config.getString("clanSettings.teleport.bossbar.colors.high"));
        }
        if (percent > 0.3) {
            return BarColor.valueOf((String)config.getString("clanSettings.teleport.bossbar.colors.medium"));
        }
        return BarColor.valueOf((String)config.getString("clanSettings.teleport.bossbar.colors.low"));
    }

    private int getDuration(Player player) {
        User user = this.luckPerms.getPlayerAdapter(Player.class).getUser(player);
        return Bastion.getInstance().getConfig().getInt("clanSettings.teleport.groups." + user.getPrimaryGroup(), 7);
    }
}
