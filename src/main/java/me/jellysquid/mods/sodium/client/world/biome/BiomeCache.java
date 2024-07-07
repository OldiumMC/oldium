package me.jellysquid.mods.sodium.client.world.biome;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import java.util.Arrays;

public class BiomeCache {
    private final World world;

    private final Biome[] biomes;

    public BiomeCache(World world) {
        this.world = world;
        this.biomes = new Biome[16 * 16];
    }

    public Biome getBiome(int x, int y, int z) {
        int idx = ((z & 15) << 4) | (x & 15);

        Biome biome = biomes[idx];

        if (biome == null) {
            biomes[idx] = biome = world.getBiome(new BlockPos(x, y, z));
        }

        return biome;
    }

    public void reset() {
        Arrays.fill(biomes, null);
    }
}