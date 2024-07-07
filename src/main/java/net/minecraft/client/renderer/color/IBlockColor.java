package net.minecraft.client.renderer.color;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public interface IBlockColor {
    int colorMultiplier(BlockState state, BlockView worldIn, BlockPos pos, int tintIndex);
}