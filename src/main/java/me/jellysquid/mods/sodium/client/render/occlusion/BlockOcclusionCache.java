package me.jellysquid.mods.sodium.client.render.occlusion;

import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;

public class BlockOcclusionCache {
    private final BlockPos.Mutable cpos = new BlockPos.Mutable(0, 0, 0);

    public BlockOcclusionCache() {
    }

    /**
     * @param view   The world view for this render context
     * @param pos    The position of the block
     * @param facing The facing direction of the side to check
     * @return True if the block side facing {@param dir} is not occluded, otherwise false
     */
    public boolean shouldDrawSide(BlockView view, BlockPos pos, Direction facing) {
        BlockPos.Mutable adjPos = this.cpos;
        adjPos.setPosition(
                pos.getX() + facing.getOffsetX(),
                pos.getY() + facing.getOffsetY(),
                pos.getZ() + facing.getOffsetZ()
        );

        Block self = view.getBlockState(pos).getBlock();

        return self.isSideInvisible(view, adjPos, facing);
    }
}