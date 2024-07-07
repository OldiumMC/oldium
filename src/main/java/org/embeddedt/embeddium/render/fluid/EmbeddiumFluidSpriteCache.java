package org.embeddedt.embeddium.render.fluid;

import me.jellysquid.mods.sodium.mixin.features.chunk_rendering.AccessorBlockFluidRenderer;
import me.jellysquid.mods.sodium.mixin.features.chunk_rendering.AccessorBlockRenderDispatcher;
import net.minecraft.block.AbstractFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;

public class EmbeddiumFluidSpriteCache {
    private final Sprite[] waterSprites, lavaSprites;

    public EmbeddiumFluidSpriteCache() {
        AccessorBlockFluidRenderer fluidRenderer = (AccessorBlockFluidRenderer)((AccessorBlockRenderDispatcher)MinecraftClient.getInstance().getBlockRenderManager()).getFluidRenderer();
        waterSprites = fluidRenderer.getWaterSprites();
        lavaSprites = fluidRenderer.getLavaSprites();
    }

    public Sprite[] getSprites(AbstractFluidBlock fluid) {
        if (fluid.getMaterial() == Material.WATER) {
            return waterSprites;
        }
        return lavaSprites;
    }
}