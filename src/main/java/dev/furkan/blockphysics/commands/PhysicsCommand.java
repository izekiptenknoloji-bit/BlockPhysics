package dev.furkan.blockphysics.commands;

import dev.furkan.blockphysics.BlockPhysicsPlugin;
import dev.furkan.blockphysics.ConfigManager;
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
            sender.sendMessage(ChatColor.YELLOW + "Kullanim: /blockphysics <reload|toggle|status>");
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
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Kullanim: /blockphysics <reload|toggle|status>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("reload", "toggle", "status")
                    .filter(option -> option.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
