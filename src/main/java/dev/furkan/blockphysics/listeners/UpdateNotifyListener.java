package dev.furkan.blockphysics.listeners;

import dev.furkan.blockphysics.BlockPhysicsPlugin;
import dev.furkan.blockphysics.UpdateChecker.RemoteRelease;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UpdateNotifyListener implements Listener {

    private final BlockPhysicsPlugin plugin;

    public UpdateNotifyListener(BlockPhysicsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfigManager().isNotifyOpsOnJoin()) {
            return;
        }

        RemoteRelease update = plugin.getAvailableUpdate();
        if (update == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("blockphysics.admin")) {
            return;
        }

        player.sendMessage(ChatColor.GOLD + "[BlockPhysics] " + ChatColor.YELLOW + "Yeni surum mevcut: "
                + ChatColor.WHITE + update.version() + ChatColor.YELLOW + " -> " + ChatColor.AQUA + update.url());

        if (plugin.isUpdateStaged()) {
            player.sendMessage(ChatColor.GOLD + "[BlockPhysics] " + ChatColor.YELLOW
                    + "Guncelleme indirildi, sunucu yeniden baslatildiginda otomatik kurulacak.");
        }
    }
}
