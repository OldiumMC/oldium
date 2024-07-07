package me.jellysquid.mods.sodium.mixin.features.fast_biome_colors;

import me.jellysquid.mods.sodium.client.model.quad.blender.BlockColorSettings;
import me.jellysquid.mods.sodium.client.model.quad.blender.DefaultBlockColorSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public class MixinBlock implements BlockColorSettings<BlockState> {

    @Override
    public boolean oldium$useSmoothColorBlending(BlockView view, BlockState state, BlockPos pos) {
        return DefaultBlockColorSettings.isSmoothBlendingAvailable(state.getBlock());
    }
}