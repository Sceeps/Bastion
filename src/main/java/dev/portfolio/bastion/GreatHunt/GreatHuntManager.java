package dev.portfolio.bastion.GreatHunt;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class GreatHuntManager {
    private Player target;
    private boolean active = false;
    private BossBar bossBar;
    private int timeLeft = 1800;
    private BukkitRunnable mainTask;
    private BukkitTask schedulerTask;
    private BukkitTask announceTask;
    private static final int MIN_PLAYERS = 2;

    public void startScheduler() {
        if (this.schedulerTask != null) {
            this.schedulerTask.cancel();
            this.schedulerTask = null;
        }
        if (this.announceTask != null) {
            this.announceTask.cancel();
            this.announceTask = null;
        }

        String timeString = Bastion.getInstance().getConfig().getString("huntSettings.time", "15:00");
        LocalTime targetTime = LocalTime.parse(timeString);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime targetDateTime = now.with(targetTime);
        if (targetDateTime.isBefore(now)) {
            targetDateTime = targetDateTime.plusDays(1L);
        }
        long seconds = ChronoUnit.SECONDS.between(now, targetDateTime);
        if (seconds <= 1L) {
            targetDateTime = targetDateTime.plusDays(1L);
            seconds = ChronoUnit.SECONDS.between(now, targetDateTime);
        }
        int secondsBeforeAnnounce = Bastion.getInstance().getConfig().getInt(
                "huntSettings.secondsBeforeAnnounce",
                Bastion.getInstance().getConfig().getInt("settings.secondsBeforeAnnounce", 180));
        List<String> message = Bastion.getInstance().getConfig().getStringList("messages.announce");
        if (seconds > (long) secondsBeforeAnnounce) {
            long announceDelay = (seconds - (long) secondsBeforeAnnounce) * 20L;
            this.announceTask = Bukkit.getScheduler().runTaskLater(Bastion.getInstance(), () -> {
                for (String line : message) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(Utils.color(line));
                    }
                }
            }, announceDelay);
        }
        Bukkit.getLogger().info("[GreatHunt] запуск через " + seconds + " секунд");
        this.schedulerTask = Bukkit.getScheduler().runTaskLater(
                Bastion.getInstance(), this::startEvent, seconds * 20L);
    }

    public void startEvent() {
        if (!Bastion.getInstance().getConfig().getBoolean("huntSettings.enable")) {
            Bukkit.getLogger().info("[GreatHunt] отключен, пропуск запуска");
            this.startScheduler();
            return;
        }
        if (this.active) {
            return;
        }
        List<String> blacklist = Bastion.getInstance().getConfig().getStringList("huntSettings.blacklistWorld");
        ArrayList<Player> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (blacklist.contains(p.getWorld().getName())) continue;
            players.add(p);
        }
        if (players.size() < MIN_PLAYERS) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.noOnline")));
            }
            this.startScheduler();
            return;
        }
        this.target = players.get(new Random().nextInt(players.size()));
        this.active = true;
        this.timeLeft = 1800;
        this.startBossBar();
        this.sendStartMessages();
        this.startMainTask();
    }

    private void startBossBar() {
        this.bossBar = Bukkit.createBossBar("Охота", BarColor.RED, BarStyle.SOLID, new BarFlag[0]);
        this.bossBar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.bossBar.addPlayer(p);
        }
    }

    private void sendStartMessages() {
        String titleHunters = Utils.color(Bastion.getInstance().getConfig().getString("messages.titleToHunters")
                .replace("{target}", this.target.getName()));
        String subHunters = Utils.color(Bastion.getInstance().getConfig().getString("messages.subtitleToHunters"));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != this.target) {
                player.sendTitle(titleHunters, subHunters, 10, 70, 20);
            }
            Utils.sound(player, "huntSettings.startSound");
        }
        this.target.sendTitle(
                Utils.color(Bastion.getInstance().getConfig().getString("messages.titleToTarget")),
                Utils.color(Bastion.getInstance().getConfig().getString("messages.subtitleToTarget")),
                10, 70, 20);
        this.target.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.instruction")));
    }

    private void startMainTask() {
        if (this.mainTask != null) {
            this.mainTask.cancel();
        }
        this.mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!GreatHuntManager.this.active) {
                    this.cancel();
                    return;
                }
                if (GreatHuntManager.this.timeLeft <= 0) {
                    GreatHuntManager.this.winTarget();
                    this.cancel();
                    return;
                }
                GreatHuntManager.this.updateBossBar();
                GreatHuntManager.this.giveEffects();
                if (GreatHuntManager.this.timeLeft % 60 == 0) {
                    GreatHuntManager.this.sendCoordinates();
                }
                --GreatHuntManager.this.timeLeft;
            }
        };
        this.mainTask.runTaskTimer(Bastion.getInstance(), 0L, 20L);
    }

    private void cancelMainTask() {
        if (this.mainTask != null) {
            this.mainTask.cancel();
            this.mainTask = null;
        }
    }

    private void endHuntUi() {
        this.active = false;
        this.cancelMainTask();
        if (this.bossBar != null) {
            this.bossBar.removeAll();
        }
    }

    private void updateBossBar() {
        if (this.bossBar == null) {
            return;
        }
        double progress = Math.max(0.0, Math.min(1.0, (double) this.timeLeft / 1800.0));
        this.bossBar.setProgress(progress);
        int minutes = this.timeLeft / 60;
        int seconds = this.timeLeft % 60;
        String time = String.format("%02d:%02d", minutes, seconds);
        this.bossBar.setTitle(Utils.color(Bastion.getInstance().getConfig()
                .getString("huntSettings.bossbar.title").replace("{time}", time)));
    }

    private void giveEffects() {
        if (this.target == null) {
            return;
        }
        this.target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0));
        this.target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0));
        this.target.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 40, 1));
    }

    private void sendCoordinates() {
        if (this.target == null) {
            return;
        }
        Location loc = this.target.getLocation();
        if (loc.getWorld() == null) {
            return;
        }
        String msg = Utils.color(Bastion.getInstance().getConfig().getString("messages.coordinates")
                .replace("{target}", this.target.getName())
                .replace("{world}", loc.getWorld().getName())
                .replace("{x}", String.valueOf(loc.getBlockX()))
                .replace("{y}", String.valueOf(loc.getBlockY()))
                .replace("{z}", String.valueOf(loc.getBlockZ())));
        Bukkit.broadcastMessage(msg);
    }

    private Economy economy() {
        var reg = Bastion.getInstance().getServer().getServicesManager().getRegistration(Economy.class);
        return reg != null ? reg.getProvider() : null;
    }

    public void killTarget(Player killer) {
        if (!this.active || this.target == null) {
            return;
        }
        String targetName = this.target.getName();
        this.endHuntUi();
        Economy econ = this.economy();
        if (econ != null) {
            double reward = Bastion.getInstance().getConfig().getDouble("huntSettings.reward.killTarget", 50000.0);
            econ.depositPlayer(killer, reward);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.titleTargetDeath")),
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.subtitleTargetDeath")
                            .replace("{target}", targetName)),
                    10, 70, 20);
            player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.targetLose")
                    .replace("{target}", targetName)
                    .replace("{player}", killer.getName())));
        }
        this.target = null;
        this.startScheduler();
    }

    private void winTarget() {
        if (!this.active || this.target == null) {
            return;
        }
        String targetName = this.target.getName();
        Player winner = this.target;
        this.endHuntUi();
        Economy econ = this.economy();
        if (econ != null) {
            double reward = Bastion.getInstance().getConfig().getDouble("huntSettings.reward.winTarget", 50000.0);
            econ.depositPlayer(winner, reward);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.titleTargetWin")
                            .replace("{target}", targetName)),
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.subtitleTargetWin")),
                    10, 70, 20);
            player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.targetWin")
                    .replace("{target}", targetName)));
        }
        this.target = null;
        this.startScheduler();
    }

    public void targetLeft() {
        if (!this.active || this.target == null) {
            return;
        }
        OfflinePlayer leaving = this.target;
        this.endHuntUi();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.titleTargetLeave")),
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.subtitleTargetLeave")),
                    10, 70, 20);
        }
        Economy econ = this.economy();
        if (econ != null) {
            double price = econ.getBalance(leaving) / 2.0;
            if (price > 0) {
                econ.withdrawPlayer(leaving, price);
            }
        }
        this.target = null;
        this.startScheduler();
    }

    public void targetDiedNatural() {
        if (!this.active || this.target == null) {
            return;
        }
        this.endHuntUi();
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.titleTargetDeathNoKiller")),
                    Utils.color(Bastion.getInstance().getConfig().getString("messages.subtitleTargetDeathNoKiller")),
                    10, 70, 20);
        }
        this.target = null;
        this.startScheduler();
    }

    public void stopAll() {
        this.active = false;
        this.cancelMainTask();
        if (this.schedulerTask != null) {
            this.schedulerTask.cancel();
            this.schedulerTask = null;
        }
        if (this.announceTask != null) {
            this.announceTask.cancel();
            this.announceTask = null;
        }
        if (this.bossBar != null) {
            this.bossBar.removeAll();
            this.bossBar = null;
        }
        this.target = null;
        this.timeLeft = 1800;
    }

    public void restartScheduler() {
        this.startScheduler();
    }

    public boolean isTarget(Player player) {
        return this.target != null && this.target.equals(player);
    }

    public boolean isActive() {
        return this.active;
    }

    public BossBar getBossBar() {
        return this.bossBar;
    }
}
