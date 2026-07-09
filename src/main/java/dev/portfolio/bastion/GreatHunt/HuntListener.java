package dev.portfolio.bastion.GreatHunt;

import java.util.List;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.GreatHunt.GreatHuntManager;
import dev.portfolio.bastion.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class HuntListener
implements Listener {
    private final GreatHuntManager manager;

    public HuntListener(GreatHuntManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player victim = (Player)entity;
        Entity entity2 = e.getDamager();
        if (!(entity2 instanceof Player)) {
            return;
        }
        Player damager = (Player)entity2;
        if (!this.manager.isActive() || !this.manager.isTarget(victim)) {
            return;
        }
        DataManager data = Bastion.getInstance().getDataManager();
        String victimClan = data.getClan(victim);
        String damagerClan = data.getClan(damager);
        if (victimClan != null && victimClan.equals(damagerClan)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player dead = e.getEntity();
        if (!this.manager.isTarget(dead)) {
            return;
        }
        Player killer = dead.getKiller();
        if (killer != null) {
            this.manager.killTarget(killer);
        } else {
            this.manager.targetDiedNatural();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (this.manager.isActive() && this.manager.getBossBar() != null) {
            this.manager.getBossBar().addPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (this.manager.isActive() && this.manager.getBossBar() != null) {
            this.manager.getBossBar().removePlayer(p);
        }
        if (this.manager.isTarget(p)) {
            this.manager.targetLeft();
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (!this.manager.isActive() || !this.manager.isTarget(p)) {
            return;
        }
        String message = e.getMessage().toLowerCase();
        List<String> blocked = Bastion.getInstance().getConfig().getStringList("huntSettings.blockedCommands");
        for (String cmd : blocked) {
            String normalized = cmd.toLowerCase();
            if (!message.startsWith(normalized)) continue;
            e.setCancelled(true);
            p.sendTitle(Utils.color(Bastion.getInstance().getConfig().getString("messages.titleToTargetBlockCommand")), Utils.color(Bastion.getInstance().getConfig().getString("messages.subtitleToTargetBlockCommand")), 10, 60, 20);
            return;
        }
    }
}
