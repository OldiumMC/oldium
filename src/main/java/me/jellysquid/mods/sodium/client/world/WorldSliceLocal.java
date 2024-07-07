package me.jellysquid.mods.sodium.client.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.LevelGeneratorType;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper object used to defeat identity comparisons in mods. Since vanilla provides a unique object to them for each
 * subchunk, we do the same.
 */
public class WorldSliceLocal implements SodiumBlockAccess {
    private final SodiumBlockAccess view;

    public WorldSliceLocal(SodiumBlockAccess view) {
        this.view = view;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return view.getBlockEntity(pos);
    }

    @Override
    public int getLight(BlockPos pos, int lightValue) {
        return view.getLight(pos, lightValue);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return view.getBlockState(pos);
    }

    @Override
    public boolean isAir(BlockPos pos) {
        return view.isAir(pos);
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return view.getBiome(pos);
    }

    @Override
    public boolean isEmpty() {
        return view.isEmpty();
    }


    @Override
    public int getStrongRedstonePower(BlockPos pos, Direction direction) {
        return view.getStrongRedstonePower(pos, direction);
    }

    @Override
    public LevelGeneratorType getGeneratorType() {
        return view.getGeneratorType();
    }

    @Override
    public int getBlockTint(BlockPos pos, BiomeColors.ColorProvider resolver) {
        return view.getBlockTint(pos, resolver);
    }
}
