package me.jellysquid.mods.sodium.client.model.quad.blender;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

public interface BlockColorSettings<T> {
    /**
     * Configures whether biome colors from a color provider will be interpolated for this block. You should only
     * enable this functionality if your color provider returns values based upon a pair of coordinates in the world,
     * and not if it needs access to the block state itself.
     *
     * @return True if interpolation should be used, otherwise false.
     */
    boolean oldium$useSmoothColorBlending(BlockView view, T state, BlockPos pos);

    @SuppressWarnings("unchecked")
    static boolean isSmoothBlendingEnabled(BlockView world, BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof BlockColorSettings) {
        	BlockColorSettings<BlockState> settings = (BlockColorSettings<BlockState>) state.getBlock();
            return settings.oldium$useSmoothColorBlending(world, state, pos);
        }

        return false;
    }
}