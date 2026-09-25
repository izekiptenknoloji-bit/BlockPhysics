package dev.furkan.blockphysics;

import org.bukkit.Material;

final class FallingState {

    private final Material material;
    private final int cascadeDepth;
    private int slideAttempts;

    FallingState(Material material, int cascadeDepth) {
        this.material = material;
        this.cascadeDepth = cascadeDepth;
        this.slideAttempts = 0;
    }

    Material getMaterial() {
        return material;
    }

    int getCascadeDepth() {
        return cascadeDepth;
    }

    int getSlideAttempts() {
        return slideAttempts;
    }

    void incrementSlideAttempts() {
        slideAttempts++;
    }
}
