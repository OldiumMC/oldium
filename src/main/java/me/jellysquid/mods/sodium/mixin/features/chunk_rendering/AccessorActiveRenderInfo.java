package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.nio.FloatBuffer;
import net.minecraft.client.render.Camera;

@Mixin(Camera.class)
public interface AccessorActiveRenderInfo {
    @Accessor("PROJECTION_MATRIX")
    static FloatBuffer getProjectionMatrix() {
        throw new RuntimeException("mixin");
    }

    @Accessor("MODEL_MATRIX")
    static FloatBuffer getModelMatrix() {
        throw new RuntimeException("mixin");
    }
}
