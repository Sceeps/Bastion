package dev.portfolio.bastion.Privates;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class ToggleManager {
    private final Map<UUID, Boolean> map = new HashMap<UUID, Boolean>();

    public boolean isEnabled(Player p) {
        return this.map.getOrDefault(p.getUniqueId(), false);
    }

    public void toggle(Player p) {
        this.map.put(p.getUniqueId(), !this.isEnabled(p));
    }
}
