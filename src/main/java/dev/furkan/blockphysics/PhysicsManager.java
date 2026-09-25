package dev.furkan.blockphysics;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Blok destegini kaybettiginde gercek bir FallingBlock varligina donusturur,
 * dumduz olmayan bir yuzeye inerse acik kenarlara dogru kaydirir ve
 * uygun bir yerde gercek bloga geri cevirir.
 */
public final class PhysicsManager {

    private static final BlockFace[] HORIZONTAL_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private final BlockPhysicsPlugin plugin;
    private final ConfigManager config;
    private final Map<UUID, FallingState> tracked = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public PhysicsManager(BlockPhysicsPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void scheduleCheck(Block block) {
        if (!config.isEnabled()) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> triggerFall(block, 0));
    }

    public void triggerFall(Block block) {
        triggerFall(block, 0);
    }

    private void triggerFall(Block block, int cascadeDepth) {
        if (!config.isEnabled() || cascadeDepth > config.getMaxCascadePerEvent()) {
            return;
        }

        Material type = block.getType();
        if (!type.isSolid() || !config.isAffected(type)) {
            return;
        }
        if (isSupported(block)) {
            return;
        }

        BlockData data = block.getBlockData().clone();
        Location spawnLoc = block.getLocation().add(0.5, 0.0, 0.5);

        block.setType(Material.AIR, false);

        FallingBlock fallingBlock = block.getWorld().spawnFallingBlock(spawnLoc, data);
        fallingBlock.setDropItem(false);
        fallingBlock.setHurtEntities(false);
        fallingBlock.setVelocity(new Vector(0, 0, 0));

        tracked.put(fallingBlock.getUniqueId(), new FallingState(type, cascadeDepth));

        Block above = block.getRelative(BlockFace.UP);
        Bukkit.getScheduler().runTask(plugin, () -> triggerFall(above, cascadeDepth + 1));
    }

    public void handleLanding(EntityChangeBlockEvent event) {
        if (event.getEntity().getType() != EntityType.FALLING_BLOCK) {
            return;
        }

        UUID id = event.getEntity().getUniqueId();
        FallingState state = tracked.get(id);
        if (state == null) {
            return;
        }

        Block landingBlock = event.getBlock();
        FallingBlock fallingBlock = (FallingBlock) event.getEntity();

        List<BlockFace> openEdges = findOpenEdges(landingBlock);

        // Dort tarafi da acik oldugunda (tek genislikteki bir direk/yigin tepesi) kaymak
        // yerine oldugu yerde yerlesir; aksi halde yalnizca izole kule uclari bile surekli
        // etrafa savrulup ust uste istiflenemezdi. Kismi egimlerde (1-3 taraf acik) kayma
        // hala gecerli.
        boolean isolatedPeak = openEdges.size() == HORIZONTAL_FACES.length;

        if (!openEdges.isEmpty() && !isolatedPeak && state.getSlideAttempts() < config.getMaxSlideAttempts()) {
            event.setCancelled(true);
            state.incrementSlideAttempts();

            BlockFace direction = openEdges.get(random.nextInt(openEdges.size()));
            Vector push = new Vector(direction.getModX() * 0.32, 0.08, direction.getModZ() * 0.32);
            fallingBlock.setVelocity(push);
            return;
        }

        tracked.remove(id);

        Material currentType = landingBlock.getType();
        boolean spaceFree = !currentType.isSolid();

        if (!spaceFree) {
            event.setCancelled(true);
            fallingBlock.remove();
            if (config.isDropItemIfNoSpace()) {
                landingBlock.getWorld().dropItemNaturally(landingBlock.getLocation().add(0.5, 0.5, 0.5),
                        new ItemStack(state.getMaterial()));
            }
            return;
        }

        playLandingEffects(landingBlock.getLocation());
    }

    private void playLandingEffects(Location location) {
        if (config.isSoundEffects()) {
            location.getWorld().playSound(location, Sound.BLOCK_STONE_PLACE, 0.7f, 0.9f);
        }
        if (config.isParticleEffects()) {
            location.getWorld().spawnParticle(Particle.BLOCK, location.clone().add(0.5, 0.3, 0.5),
                    10, 0.25, 0.15, 0.25, Material.STONE.createBlockData());
        }
    }

    private List<BlockFace> findOpenEdges(Block landingBlock) {
        List<BlockFace> edges = new ArrayList<>(4);
        for (BlockFace face : HORIZONTAL_FACES) {
            Block neighbor = landingBlock.getRelative(face);
            Block belowNeighbor = neighbor.getRelative(BlockFace.DOWN);
            if (!neighbor.getType().isSolid() && !belowNeighbor.getType().isSolid()) {
                edges.add(face);
            }
        }
        return edges;
    }

    private boolean isSupported(Block block) {
        return block.getRelative(BlockFace.DOWN).getType().isSolid();
    }

    public void shutdown() {
        for (Map.Entry<UUID, FallingState> entry : tracked.entrySet()) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            Location loc = entity.getLocation();
            Block block = loc.getBlock();
            entity.remove();
            if (!block.getType().isSolid()) {
                block.setType(entry.getValue().getMaterial(), false);
            } else {
                block.getWorld().dropItemNaturally(loc, new ItemStack(entry.getValue().getMaterial()));
            }
        }
        tracked.clear();
    }
}
