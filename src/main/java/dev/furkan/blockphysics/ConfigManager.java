package dev.furkan.blockphysics;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;

public final class ConfigManager {

    private final BlockPhysicsPlugin plugin;

    private boolean enabled;
    private boolean whitelistMode;
    private Set<Material> configuredBlocks;

    private int maxSlideAttempts;
    private int maxCascadePerEvent;
    private boolean dropItemIfNoSpace;

    private boolean soundEffects;
    private boolean particleEffects;

    public ConfigManager(BlockPhysicsPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.enabled = config.getBoolean("enabled", true);

        String mode = config.getString("blocks.mode", "whitelist");
        this.whitelistMode = !mode.equalsIgnoreCase("blacklist");

        this.configuredBlocks = EnumSet.noneOf(Material.class);
        for (String name : config.getStringList("blocks.list")) {
            Material material = Material.matchMaterial(name);
            if (material != null) {
                configuredBlocks.add(material);
            } else {
                plugin.getLogger().log(Level.WARNING, "Gecersiz blok adi config.yml icinde: " + name);
            }
        }

        this.maxSlideAttempts = Math.max(0, config.getInt("physics.max-slide-attempts", 8));
        this.maxCascadePerEvent = Math.max(1, config.getInt("physics.max-cascade-per-event", 200));
        this.dropItemIfNoSpace = config.getBoolean("physics.drop-item-if-no-space", true);

        this.soundEffects = config.getBoolean("effects.sound", true);
        this.particleEffects = config.getBoolean("effects.particles", true);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAffected(Material material) {
        boolean inList = configuredBlocks.contains(material);
        return whitelistMode ? inList : (!inList && material.isSolid());
    }

    public int getAffectedBlockCount() {
        return configuredBlocks.size();
    }

    public int getMaxSlideAttempts() {
        return maxSlideAttempts;
    }

    public int getMaxCascadePerEvent() {
        return maxCascadePerEvent;
    }

    public boolean isDropItemIfNoSpace() {
        return dropItemIfNoSpace;
    }

    public boolean isSoundEffects() {
        return soundEffects;
    }

    public boolean isParticleEffects() {
        return particleEffects;
    }
}
