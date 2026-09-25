package dev.furkan.blockphysics;

import dev.furkan.blockphysics.UpdateChecker.RemoteRelease;
import dev.furkan.blockphysics.commands.PhysicsCommand;
import dev.furkan.blockphysics.listeners.BlockChangeListener;
import dev.furkan.blockphysics.listeners.UpdateNotifyListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlockPhysicsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PhysicsManager physicsManager;
    private UpdateChecker updateChecker;
    private volatile RemoteRelease availableUpdate;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.physicsManager = new PhysicsManager(this, configManager);

        getServer().getPluginManager().registerEvents(new BlockChangeListener(physicsManager), this);
        getServer().getPluginManager().registerEvents(new UpdateNotifyListener(this), this);

        PhysicsCommand command = new PhysicsCommand(this, configManager);
        getCommand("blockphysics").setExecutor(command);
        getCommand("blockphysics").setTabCompleter(command);

        setupUpdateChecker();

        getLogger().info("BlockPhysics etkinlestirildi. " + configManager.getAffectedBlockCount() + " blok turu izleniyor.");
    }

    @Override
    public void onDisable() {
        if (physicsManager != null) {
            physicsManager.shutdown();
        }
    }

    private void setupUpdateChecker() {
        if (!configManager.isUpdateCheckEnabled()) {
            this.updateChecker = null;
            return;
        }

        this.updateChecker = new UpdateChecker(this, configManager.getUpdateRepository());
        runUpdateCheck();

        long intervalTicks = configManager.getUpdateCheckIntervalHours() * 60L * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, this::runUpdateCheck, intervalTicks, intervalTicks);
    }

    private void runUpdateCheck() {
        if (updateChecker == null) {
            return;
        }
        updateChecker.checkAsync(release -> {
            if (release == null) {
                return;
            }
            if (UpdateChecker.isNewer(release.version(), getPluginMeta().getVersion())) {
                this.availableUpdate = release;
                getLogger().warning("Yeni bir BlockPhysics surumu mevcut: " + release.version()
                        + " (mevcut: " + getPluginMeta().getVersion() + ") -> " + release.url());
            }
        });
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public RemoteRelease getAvailableUpdate() {
        return availableUpdate;
    }

    public void setAvailableUpdate(RemoteRelease availableUpdate) {
        this.availableUpdate = availableUpdate;
    }

    public PhysicsManager getPhysicsManager() {
        return physicsManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
