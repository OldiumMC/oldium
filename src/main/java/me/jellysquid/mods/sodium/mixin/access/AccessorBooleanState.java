package me.jellysquid.mods.sodium.mixin.access;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.BooleanState.class)
public interface AccessorBooleanState {
    @Accessor
    boolean getCachedState();
}
