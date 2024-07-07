package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.FluidRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRenderManager.class)
public interface AccessorBlockRenderDispatcher {
    @Accessor
    FluidRenderer getFluidRenderer();
}
