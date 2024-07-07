package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

class ConfigurableColorBlender implements BiomeColorBlender {
    private final BiomeColorBlender defaultBlender;
    private final BiomeColorBlender smoothBlender;

    public ConfigurableColorBlender(MinecraftClient client) {
        this.defaultBlender = new FlatBiomeColorBlender();
        this.smoothBlender = isSmoothBlendingEnabled() ? new SmoothBiomeColorBlender() : this.defaultBlender;
    }

    private static boolean isSmoothBlendingEnabled() {
        return SodiumClientMod.options().quality.biomeBlendRadius > 0;
    }

    @Override
    public int[] getColors(IBlockColor colorizer, BlockView world, BlockState state, BlockPos origin,
                           ModelQuadView quad) {
    	BiomeColorBlender blender;

        if (BlockColorSettings.isSmoothBlendingEnabled(world, state, origin)) {
            blender = this.smoothBlender;
        } else {
            blender = this.defaultBlender;
        }

        return blender.getColors(colorizer, world, state, origin, quad);
    }

}