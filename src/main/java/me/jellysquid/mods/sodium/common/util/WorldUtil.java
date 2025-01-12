package me.jellysquid.mods.sodium.common.util;

import net.minecraft.block.AbstractFluidBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.joml.Vector3d;

/**
 * Contains methods stripped from BlockState or FluidState that didn't actually need to be there. Technically these
 * could be a mixin to Block or Fluid, but that's annoying while not actually providing any benefit.
 */
public class WorldUtil {

    public static Vector3d getVelocity(BlockView world, BlockPos pos, BlockState thizz) {
        Vector3d velocity = new Vector3d();
        int decay = getEffectiveFlowDecay(world, pos, thizz);
        BlockPos.Mutable mutable = new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ());

        for (Direction dire : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            int adjX = pos.getX() + dire.getOffsetX();
            int adjZ = pos.getZ() + dire.getOffsetZ();
            mutable.setPosition(adjX, pos.getY(), adjZ);

            int adjDecay = getEffectiveFlowDecay(world, mutable, thizz);

            if (adjDecay < 0) {
                if (!world.getBlockState(mutable).getBlock().getMaterial().blocksMovement()) {
                    adjDecay = getEffectiveFlowDecay(world, mutable.down(), thizz);

                    if (adjDecay >= 0) {
                        adjDecay -= (decay - 8);
                        velocity = velocity.add((adjX - pos.getX()) * adjDecay, 0, (adjZ - pos.getZ()) * adjDecay);
                    }
                }
            } else {
                adjDecay -= decay;
                velocity = velocity.add((adjX - pos.getX()) * adjDecay, 0, (adjZ - pos.getZ()) * adjDecay);
            }
        }

        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.get(AbstractFluidBlock.LEVEL) >= 8) {
            if (block.hasCollision(world, pos.north(), Direction.NORTH)
                    || block.hasCollision(world, pos.south(), Direction.SOUTH)
                    || block.hasCollision(world, pos.west(), Direction.WEST)
                    || block.hasCollision(world, pos.east(), Direction.EAST)
                    || block.hasCollision(world, pos.up().south(), Direction.NORTH)
                    || block.hasCollision(world, pos.up().west(), Direction.SOUTH)
                    || block.hasCollision(world, pos.up().west(), Direction.WEST)
                    || block.hasCollision(world, pos.up().east(), Direction.EAST)) {
                velocity = velocity.normalize().add(0.0D, -6.0D, 0.0D);
            }
        }

        if (velocity.x == 0 && velocity.y == 0 && velocity.z == 0)
            return velocity.zero();
        return velocity.normalize();
    }

    /**
     * Returns true if any block in a 3x3x3 cube is not the same fluid and not an opaque full cube.
     * Equivalent to FluidState::method_15756 in modern.
     */
    public static boolean method_15756(BlockView world, BlockPos pos, AbstractFluidBlock fluid) {
        for (int i = 0; i < 2; ++i) {
            for (int j = 0; j < 2; ++j) {
                BlockState block = world.getBlockState(pos);
                if (!block.getBlock().hasTransparency() && getFluid(block) != fluid) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns fluid height as a percentage of the block; 0 is none and 1 is full.
     */
    public static float getFluidHeight(AbstractFluidBlock fluid, int meta) {
        return fluid == null ? 0 : 1 - AbstractFluidBlock.getHeightPercent(meta);
    }

    /**
     * Returns the flow decay but converts values indicating falling liquid (values >=8) to their effective source block
     * value of zero
     */
    public static int getEffectiveFlowDecay(BlockView world, BlockPos pos, BlockState thiz) {
        if (world.getBlockState(pos).getBlock().getMaterial() != thiz.getBlock().getMaterial()) {
            return -1;
        } else {
            int decay = thiz.get(AbstractFluidBlock.LEVEL);
            return decay >= 8 ? 0 : decay;
        }
    }

    // I believe forge mappings in modern say BreakableBlock, while yarn says TransparentBlock.
    // I have a sneaking suspicion isOpaque is neither, but it works for now
    public static boolean shouldDisplayFluidOverlay(BlockState block) {
        return !block.getBlock().getMaterial().isOpaque() || block.getBlock().getMaterial() == Material.FOLIAGE;
    }

    public static AbstractFluidBlock getFluid(BlockState b) {
        return toFluidBlock(b.getBlock());
    }

    /**
     * Equivalent to method_15748 in 1.16.5
     */
    public static boolean isEmptyOrSame(AbstractFluidBlock fluid, AbstractFluidBlock otherFluid) {
        return otherFluid == null || fluid == otherFluid;
    }

    /**
     * Equivalent to method_15749 in 1.16.5
     */
    public static boolean method_15749(BlockView world, AbstractFluidBlock thiz, BlockPos pos, Direction dir) {
        BlockState b = world.getBlockState(pos);
        AbstractFluidBlock f = getFluid(b);
        if (f == thiz) {
            return false;
        }
        if (dir == Direction.UP) {
            return true;
        }
        return b.getBlock().getMaterial() != Material.ICE && b.getBlock().hasCollision(world, pos, dir);
    }

    public static AbstractFluidBlock toFluidBlock(Block block) {
        if(block instanceof AbstractFluidBlock) {
            return (AbstractFluidBlock)block;
        }
        return null;
    }

    public static AbstractFluidBlock getFluidOfBlock(Block block) {
        return toFluidBlock(block);
    }
}