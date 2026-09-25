package dev.furkan.blockphysics;

import dev.furkan.blockphysics.commands.PhysicsCommand;
import dev.furkan.blockphysics.listeners.BlockChangeListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockPhysicsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PhysicsManager physicsManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.physicsManager = new PhysicsManager(this, configManager);

        getServer().getPluginManager().registerEvents(new BlockChangeListener(physicsManager), this);

        PhysicsCommand command = new PhysicsCommand(this, configManager);
        getCommand("blockphysics").setExecutor(command);
        getCommand("blockphysics").setTabCompleter(command);

        getLogger().info("BlockPhysics etkinlestirildi. " + configManager.getAffectedBlockCount() + " blok turu izleniyor.");
    }

    @Override
    public void onDisable() {
        if (physicsManager != null) {
            physicsManager.shutdown();
        }
    }

    public PhysicsManager getPhysicsManager() {
        return physicsManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
