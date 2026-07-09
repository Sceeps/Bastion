package dev.portfolio.bastion.Clans;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class FriendlyFireListener implements Listener {
    private final DataManager data;

    public FriendlyFireListener(DataManager data) {
        this.data = data;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player damager = resolveDamager(event.getDamager());
        if (damager == null) {
            return;
        }
        String victimClan = this.data.getClan(victim);
        String damagerClan = this.data.getClan(damager);
        if (victimClan == null || damagerClan == null) {
            return;
        }
        if (!victimClan.equals(damagerClan)) {
            return;
        }
        if (!this.data.isClanPvpEnabled(victimClan)) {
            event.setCancelled(true);
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
