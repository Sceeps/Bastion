package dev.portfolio.bastion;

import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.portfolio.bastion.Bastion;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public class Utils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

    public static String color(String message) {
        if (message == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group();
            String color = ChatColor.of((String)hexCode.substring(1)).toString();
            matcher.appendReplacement(buffer, color);
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)buffer.toString());
    }

    public static String color(String message, OfflinePlayer player) {
        if (message == null) {
            return "";
        }
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            message = PlaceholderAPI.setPlaceholders((OfflinePlayer)player, (String)message);
        }
        return Utils.color(message);
    }

    public static ItemStack createHead(String base64) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta)head.getItemMeta();
        PlayerProfile profile = Bukkit.createPlayerProfile((UUID)UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();
        try {
            String decoded = new String(Base64.getDecoder().decode(base64));
            String url = decoded.split("\"url\":\"")[1].split("\"")[0];
            textures.setSkin(new URL(url));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        profile.setTextures(textures);
        meta.setOwnerProfile(profile);
        head.setItemMeta((ItemMeta)meta);
        return head;
    }

    public static void sound(Player player, String path) {
        String soundName = Bastion.getInstance().getConfig().getString(path);
        if (soundName == null) {
            return;
        }
        try {
            Sound sound = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft((String)soundName.toLowerCase()));
            if (sound == null) {
                return;
            }
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
        catch (IllegalArgumentException e) {
            player.sendMessage("§cНеверное название звука в конфиге: " + soundName);
        }
    }
}
