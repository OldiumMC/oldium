package me.jellysquid.mods.sodium.mixin.features.block;

import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheShared;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinWorldRenderer {
    /**
     * Reset any global cached state before rendering a frame. This will hopefully ensure that any world state that has
     * changed is reflected in vanilla-style rendering.
     */
    @Inject(method = "renderWorld(FJ)V", at = @At("HEAD"))
    private void reset(float partialTicks, long finishTimeNano, CallbackInfo ci) {
        ChunkRenderCacheShared.resetCaches();
    }
}
