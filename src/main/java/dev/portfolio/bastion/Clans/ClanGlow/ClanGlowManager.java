package dev.portfolio.bastion.Clans.ClanGlow;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedParticle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import dev.portfolio.bastion.Clans.DataManager;
import dev.portfolio.bastion.Bastion;
import dev.portfolio.bastion.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ClanGlowManager {
    private final DataManager data;
    private final ProtocolManager protocolManager;
    private final Set<String> activeClans = new HashSet<String>();

    public ClanGlowManager(DataManager data) {
        this.data = data;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void toggleLocator(Player player) {
        String clan = this.data.getClan(player);
        if (clan == null) {
            return;
        }
        String owner = this.data.getOwner(clan);
        if (owner == null || !owner.equals(player.getUniqueId().toString())) {
            player.sendMessage(Utils.color(Bastion.getInstance().getConfig().getString("messages.onlyOwnerCanInvite"), (OfflinePlayer) player));
            return;
        }
        if (this.activeClans.contains(clan)) {
            this.disable(clan);
            this.sendClanMessage(clan, Bastion.getInstance().getConfig().getString("messages.clanMsgGlowDisable"));
        } else {
            this.activeClans.add(clan);
            this.sendClanMessage(clan, Bastion.getInstance().getConfig().getString("messages.clanMsgGlowEnable"));
        }
    }

    private void disable(String clan) {
        this.activeClans.remove(clan);
    }

    public void disableClan(String clan) {
        this.disable(clan);
    }

    public void renameClan(String oldName, String newName) {
        if (this.activeClans.remove(oldName)) {
            this.activeClans.add(newName);
        }
    }

    private List<Player> getOnlineMembers(String clan) {
        ArrayList<Player> list = new ArrayList<Player>();
        for (String uuid : this.data.getMembers(clan)) {
            if (uuid == null || uuid.isEmpty()) continue;
            try {
                Player p = Bukkit.getPlayer((UUID)UUID.fromString(uuid));
                if (p == null) continue;
                list.add(p);
            }
            catch (Exception exception) {}
        }
        String ownerId = this.data.getOwner(clan);
        if (ownerId != null && !ownerId.isEmpty()) {
            try {
                Player owner = Bukkit.getPlayer((UUID)UUID.fromString(ownerId));
                if (owner != null) {
                    list.add(owner);
                }
            }
            catch (Exception exception) {
            }
        }
        return list;
    }

    public void startParticleTask(Bastion plugin) {
        Bukkit.getScheduler().runTaskTimer((Plugin)plugin, () -> {
            for (String clan : this.activeClans) {
                List<Player> members = this.getOnlineMembers(clan);
                for (Player viewer : members) {
                    for (Player target : members) {
                        this.sendParticlePacket(viewer, target);
                    }
                }
            }
        }, 0L, 5L);
    }

    private void sendParticlePacket(Player viewer, Player target) {
        try {
            if (viewer == null || target == null) {
                return;
            }
            if (!viewer.isOnline() || !target.isOnline()) {
                return;
            }
            Location base = target.getLocation().clone().add(0.0, 2.2, 0.0);
            double radius = 0.45;
            int points = 14;
            double time = (double)System.currentTimeMillis() / 350.0;
            Location center = base.clone().add(0.0, 0.6, 0.0);
            for (int i = 0; i < points; ++i) {
                double angle = Math.PI * 2 / (double)points * (double)i + time;
                double x = Math.cos(angle) * radius;
                double z = Math.sin(angle) * radius;
                Location loc = center.clone().add(x, 0.0, z);
                this.sendGoldDust(viewer, loc);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendGoldDust(Player viewer, Location loc) throws Exception {
        PacketContainer packet = this.protocolManager.createPacket(PacketType.Play.Server.WORLD_PARTICLES);
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB((int)0, (int)255, (int)120), 1.5f);
        WrappedParticle particle = WrappedParticle.create((Particle)Particle.DUST, (Object)dust);
        packet.getNewParticles().write(0, particle);
        packet.getDoubles().write(0, loc.getX());
        packet.getDoubles().write(1, loc.getY());
        packet.getDoubles().write(2, loc.getZ());
        packet.getFloat().write(0, Float.valueOf(0.0f));
        packet.getFloat().write(1, Float.valueOf(0.0f));
        packet.getFloat().write(2, Float.valueOf(0.0f));
        packet.getFloat().write(3, Float.valueOf(0.0f));
        packet.getIntegers().write(0, 0);
        packet.getBooleans().write(0, true);
        this.protocolManager.sendServerPacket(viewer, packet);
    }

    public boolean isActive(String clan) {
        return this.activeClans.contains(clan);
    }

    private void sendClanMessage(String clan, String message) {
        for (String uuidStr : this.data.getMembers(clan)) {
            if (uuidStr == null || uuidStr.isEmpty()) continue;
            try {
                Player p = Bukkit.getPlayer((UUID)UUID.fromString(uuidStr));
                if (p == null) continue;
                p.sendMessage(Utils.color(message, (OfflinePlayer)p));
            }
            catch (Exception exception) {}
        }
        String ownerId = this.data.getOwner(clan);
        if (ownerId != null && !ownerId.isEmpty()) {
            try {
                Player p = Bukkit.getPlayer((UUID)UUID.fromString(ownerId));
                if (p != null) {
                    p.sendMessage(Utils.color(message, (OfflinePlayer)p));
                }
            }
            catch (Exception exception) {
            }
        }
    }
}
