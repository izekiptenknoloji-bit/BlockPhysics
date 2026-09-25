package dev.furkan.blockphysics.commands;

import dev.furkan.blockphysics.BlockPhysicsPlugin;
import dev.furkan.blockphysics.ConfigManager;
import dev.furkan.blockphysics.UpdateChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PhysicsCommand implements CommandExecutor, TabCompleter {

    private final BlockPhysicsPlugin plugin;
    private final ConfigManager configManager;

    public PhysicsCommand(BlockPhysicsPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("blockphysics.admin")) {
            sender.sendMessage(ChatColor.RED + "Bu komutu kullanma izniniz yok.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Kullanim: /blockphysics <reload|toggle|status|checkupdate>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                configManager.reload();
                sender.sendMessage(ChatColor.GREEN + "BlockPhysics ayarlari yeniden yuklendi.");
            }
            case "toggle" -> {
                boolean newState = !configManager.isEnabled();
                configManager.setEnabled(newState);
                sender.sendMessage(ChatColor.GREEN + "BlockPhysics artik " + (newState ? "ACIK" : "KAPALI") + ".");
            }
            case "status" -> {
                sender.sendMessage(ChatColor.AQUA + "Durum: " + (configManager.isEnabled() ? "Acik" : "Kapali"));
                sender.sendMessage(ChatColor.AQUA + "Izlenen blok turu sayisi: " + configManager.getAffectedBlockCount());
                sender.sendMessage(ChatColor.AQUA + "Surum: " + plugin.getPluginMeta().getVersion());
                if (plugin.getAvailableUpdate() != null) {
                    sender.sendMessage(ChatColor.YELLOW + "Yeni surum mevcut: " + plugin.getAvailableUpdate().version()
                            + " -> " + plugin.getAvailableUpdate().url());
                }
            }
            case "checkupdate" -> checkUpdate(sender);
            default -> sender.sendMessage(ChatColor.YELLOW + "Kullanim: /blockphysics <reload|toggle|status|checkupdate>");
        }
        return true;
    }

    private void checkUpdate(CommandSender sender) {
        if (plugin.getUpdateChecker() == null) {
            sender.sendMessage(ChatColor.RED + "Guncelleme kontrolu devre disi (config.yml: update.enabled=false).");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "Guncelleme kontrol ediliyor...");
        plugin.getUpdateChecker().checkAsync(release -> {
            String currentVersion = plugin.getPluginMeta().getVersion();
            if (release == null) {
                sender.sendMessage(ChatColor.RED + "Guncelleme kontrolu basarisiz oldu ya da henuz bir surum yayinlanmamis.");
                return;
            }
            if (UpdateChecker.isNewer(release.version(), currentVersion)) {
                plugin.setAvailableUpdate(release);
                sender.sendMessage(ChatColor.GREEN + "Yeni surum mevcut: " + release.version() + " -> " + release.url());
            } else {
                sender.sendMessage(ChatColor.GREEN + "BlockPhysics guncel (v" + currentVersion + ").");
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("reload", "toggle", "status", "checkupdate")
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
