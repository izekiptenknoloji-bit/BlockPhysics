package dev.furkan.blockphysics.listeners;

import dev.furkan.blockphysics.PhysicsManager;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class BlockChangeListener implements Listener {

    private final PhysicsManager physicsManager;

    public BlockChangeListener(PhysicsManager physicsManager) {
        this.physicsManager = physicsManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        physicsManager.scheduleCheck(event.getBlock().getRelative(BlockFace.UP));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        physicsManager.scheduleCheck(event.getBlock().getRelative(BlockFace.UP));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        physicsManager.scheduleCheck(event.getBlock());
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            physicsManager.scheduleCheck(block.getRelative(BlockFace.UP));
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        for (Block block : event.blockList()) {
            physicsManager.scheduleCheck(block.getRelative(BlockFace.UP));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity().getType() == EntityType.FALLING_BLOCK) {
            physicsManager.handleLanding(event);
        }
    }
}
