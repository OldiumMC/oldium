package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FluidRenderer.class)
public interface AccessorBlockFluidRenderer {
    @Accessor
    Sprite[] getWaterSprites();

    @Accessor
    Sprite[] getLavaSprites();
}
