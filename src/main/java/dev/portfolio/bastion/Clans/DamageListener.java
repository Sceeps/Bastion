package dev.portfolio.bastion.Clans;

import dev.portfolio.bastion.Clans.Commands.BaseCommand;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

public class DamageListener implements Listener {
    private final BaseCommand baseCommand;

    public DamageListener(BaseCommand baseCommand) {
        this.baseCommand = baseCommand;
    }

    @EventHandler
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player damaged)) {
            return;
        }
        if (this.baseCommand.isTeleporting(damaged)) {
            this.baseCommand.cancelTeleport(damaged);
            damaged.sendMessage(Utils.color(
                    Bastion.getInstance().getConfig().getString("messages.teleportCancel"), damaged));
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player damager = resolveDamager(event.getDamager());
        if (damager == null) {
            return;
        }
        if (this.baseCommand.isTeleporting(damager)) {
            this.baseCommand.cancelTeleport(damager);
            damager.sendMessage(Utils.color(
                    Bastion.getInstance().getConfig().getString("messages.teleportCancel"), damager));
        }
    }

    private static Player resolveDamager(Entity entity) {
        if (entity instanceof Player p) {
            return p;
        }
        if (entity instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) {
                return p;
            }
        }
        return null;
    }
}
