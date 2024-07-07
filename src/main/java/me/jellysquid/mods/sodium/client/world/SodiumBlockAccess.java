package me.jellysquid.mods.sodium.client.world;

import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * Contains extensions to the vanilla {@link BlockView}.
 */
public interface SodiumBlockAccess extends BlockView {
    int getBlockTint(BlockPos pos, BiomeColors.ColorProvider resolver);
}
