package me.jellysquid.mods.sodium.mixin.access;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.FogState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface AccessorGlStateManager {
    @Accessor("FOG")
    static FogState getFogState() {
        throw new RuntimeException("mixin");
    }
}
