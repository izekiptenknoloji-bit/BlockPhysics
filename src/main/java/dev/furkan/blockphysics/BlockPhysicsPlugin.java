package dev.furkan.blockphysics;

import dev.furkan.blockphysics.UpdateChecker.RemoteRelease;
import dev.furkan.blockphysics.commands.PhysicsCommand;
import dev.furkan.blockphysics.listeners.BlockChangeListener;
import dev.furkan.blockphysics.listeners.UpdateNotifyListener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class BlockPhysicsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private PhysicsManager physicsManager;
    private UpdateChecker updateChecker;
    private volatile RemoteRelease availableUpdate;
    private volatile boolean updateStaged;

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
            if (!UpdateChecker.isNewer(release.version(), getPluginMeta().getVersion())) {
                return;
            }

            this.availableUpdate = release;
            getLogger().warning("Yeni bir BlockPhysics surumu mevcut: " + release.version()
                    + " (mevcut: " + getPluginMeta().getVersion() + ") -> " + release.url());

            if (configManager.isAutoDownloadUpdates()) {
                stageUpdate(release, null);
            }
        });
    }

    /** Guncellemeyi indirip bir sonraki sunucu yeniden baslatmasinda otomatik kurulacak sekilde hazirlar. */
    public void stageUpdate(RemoteRelease release, Runnable onDone) {
        updateChecker.downloadAndStage(release, success -> {
            if (success) {
                this.updateStaged = true;
                getLogger().warning("Guncelleme indirildi ve hazirlandi. Sunucu bir sonraki yeniden baslatmada "
                        + release.version() + " surumune otomatik gececek.");
            } else {
                getLogger().warning("Guncelleme indirilemedi, manuel kurmaniz gerekebilir: " + release.url());
            }
            if (onDone != null) {
                onDone.run();
            }
        });
    }

    public File getPluginJarFile() {
        return getFile();
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

    public boolean isUpdateStaged() {
        return updateStaged;
    }

    public PhysicsManager getPhysicsManager() {
        return physicsManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
