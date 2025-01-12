package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import me.jellysquid.mods.sodium.client.util.math.ChunkSectionPos;
import me.jellysquid.mods.sodium.client.world.biome.BiomeCache;
import me.jellysquid.mods.sodium.client.world.biome.BiomeColorCache;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.level.LevelGeneratorType;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

/**
 * Takes a slice of world state (block states, biome and light data arrays) and copies the data for use in off-thread
 * operations. This allows chunk build tasks to see a consistent snapshot of chunk data at the exact moment the task was
 * created.
 * <p>
 * World slices are not safe to use from multiple threads at once, but the data they contain is safe from modification
 * by the main client thread.
 * <p>
 * Object pooling should be used to avoid huge allocations as this class contains many large arrays.
 */
public class WorldSlice implements SodiumBlockAccess {
    private static final LightType[] LIGHT_TYPES = LightType.values();

    // The number of blocks on each axis in a section.
    private static final int SECTION_BLOCK_LENGTH = 16;

    // The number of blocks in a section.
    private static final int SECTION_BLOCK_COUNT = SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH;

    // The radius of blocks around the origin chunk that should be copied.
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;

    // The radius of chunks around the origin chunk that should be copied.
    private static final int NEIGHBOR_CHUNK_RADIUS = MathHelper.roundUp(NEIGHBOR_BLOCK_RADIUS, 16) >> 4;

    // The number of sections on each axis of this slice.
    private static final int SECTION_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The size of the lookup tables used for mapping values to coordinate int pairs. The lookup table size is always
    // a power of two so that multiplications can be replaced with simple bit shifts in hot code paths.
    private static final int TABLE_LENGTH = MathHelper.smallestEncompassingPowerOfTwo(SECTION_LENGTH);

    // The number of bits needed for each X/Y/Z component in a lookup table.
    private static final int TABLE_BITS = Integer.bitCount(TABLE_LENGTH - 1);

    // The array size for the section lookup table.
    private static final int SECTION_TABLE_ARRAY_SIZE = TABLE_LENGTH * TABLE_LENGTH * TABLE_LENGTH;

    // The world this slice has copied data from
    private final World world;
    private final LevelGeneratorType worldType;
    private final int[] defaultLightValues;

    // Local Section->Light table
    private final ChunkNibbleArray[][] lightArrays;

    // Local Section->BlockState table.
    private final BlockState[][] blockStatesArrays;

    // Local section copies. Read-only.
    private ClonedChunkSection[] sections;

    // Biome caches for each chunk section
    private final BiomeCache[] biomeCaches;

    // The biome blend caches for each color resolver type
    // This map is always re-initialized, but the caches themselves are taken from an object pool
    private final Map<BiomeColors.ColorProvider, BiomeColorCache> biomeColorCaches = new Reference2ObjectOpenHashMap<>();

    // The previously accessed and cached color resolver, used in conjunction with the cached color cache field
    private BiomeColors.ColorProvider prevColorResolver;

    // The cached lookup result for the previously accessed color resolver to avoid excess hash table accesses
    // for vertex color blending
    private BiomeColorCache prevColorCache;

    // The starting point from which this slice captures blocks
    private int baseX, baseY, baseZ;

    // The chunk origin of this slice
    @Getter
    private ChunkSectionPos origin;

    // The volume that this slice contains
    private BlockBox volume;

    public static ChunkRenderContext prepare(World world, ChunkSectionPos origin, ClonedChunkSectionCache sectionCache) {
        Chunk chunk = world.getChunk(origin.getX(), origin.getZ());
        ChunkSection section = chunk.getBlockStorage()[origin.getY()];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (section == null || section.isEmpty()) {
            return null;
        }

        BlockBox volume = new BlockBox(origin.getMinX() - NEIGHBOR_BLOCK_RADIUS,
                origin.getMinY() - NEIGHBOR_BLOCK_RADIUS,
                origin.getMinZ() - NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxX() + NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxY() + NEIGHBOR_BLOCK_RADIUS,
                origin.getMaxZ() + NEIGHBOR_BLOCK_RADIUS);

        // The min/max bounds of the chunks copied by this slice
        final int minChunkX = origin.getX() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkY = origin.getY() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkZ = origin.getZ() - NEIGHBOR_CHUNK_RADIUS;

        final int maxChunkX = origin.getX() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkY = origin.getY() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkZ = origin.getZ() + NEIGHBOR_CHUNK_RADIUS;

        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    int sectionIndex = getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ);
                    sections[sectionIndex] = sectionCache.acquire(chunkX, chunkY, chunkZ);
                }
            }
        }

        return new ChunkRenderContext(origin, sections, volume);
    }

    public WorldSlice(World context) {
        world = context;
        worldType = world.getGeneratorType();
        defaultLightValues = new int[LIGHT_TYPES.length];
        defaultLightValues[LightType.SKY.ordinal()] = world.dimension.hasNoSkylight() ? 0 : LightType.SKY.defaultValue;
        defaultLightValues[LightType.BLOCK.ordinal()] = LightType.BLOCK.defaultValue;

        sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];
        blockStatesArrays = new BlockState[SECTION_TABLE_ARRAY_SIZE][];
        biomeCaches = new BiomeCache[SECTION_TABLE_ARRAY_SIZE];
        lightArrays = new ChunkNibbleArray[SECTION_TABLE_ARRAY_SIZE][LIGHT_TYPES.length];

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int i = getLocalSectionIndex(x, y, z);

                    blockStatesArrays[i] = new BlockState[SECTION_BLOCK_COUNT];
                    Arrays.fill(blockStatesArrays[i], Blocks.AIR.getDefaultState());
                    biomeCaches[i] = new BiomeCache(world);
                }
            }
        }
    }

    public void copyData(ChunkRenderContext context) {
        origin = context.origin();
        sections = context.sections();
        volume = context.volume();

        prevColorCache = null;
        prevColorResolver = null;

        biomeColorCaches.clear();

        baseX = (origin.getX() - NEIGHBOR_CHUNK_RADIUS) << 4;
        baseY = (origin.getY() - NEIGHBOR_CHUNK_RADIUS) << 4;
        baseZ = (origin.getZ() - NEIGHBOR_CHUNK_RADIUS) << 4;

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int idx = getLocalSectionIndex(x, y, z);

                    final ClonedChunkSection section = sections[idx];

                    biomeCaches[idx].reset();

                    unpackBlockData(blockStatesArrays[idx], sections[idx], context.volume());

                    lightArrays[idx][LightType.BLOCK.ordinal()] = section.getLightArray(LightType.BLOCK);
                    lightArrays[idx][LightType.SKY.ordinal()] = section.getLightArray(LightType.SKY);
                }
            }
        }
    }

    private void unpackBlockData(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        if (origin.equals(section.getPosition())) {
            unpackBlockDataZ(states, section);
        } else {
            unpackBlockDataR(states, section, box);
        }
    }

    private static void copyBlocks(BlockState[] blocks, ClonedChunkSection section, int minBlockY, int maxBlockY, int minBlockZ, int maxBlockZ, int minBlockX, int maxBlockX) {
        for (int y = minBlockY; y <= maxBlockY; y++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int x = minBlockX; x <= maxBlockX; x++) {
                    int blockIdx = getLocalBlockIndex(x & 15, y & 15, z & 15);
                    blocks[blockIdx] = section.getBlockState(x & 15, y & 15, z & 15);
                }
            }
        }
    }

    private void unpackBlockDataR(BlockState[] states, ClonedChunkSection section, BlockBox box) {
        ChunkSectionPos pos = section.getPosition();

        int minBlockX = Math.max(box.minX, pos.getMinX());
        int maxBlockX = Math.min(box.maxX, pos.getMaxX());

        int minBlockY = Math.max(box.minY, pos.getMinY());
        int maxBlockY = Math.min(box.maxY, pos.getMaxY());

        int minBlockZ = Math.max(box.minZ, pos.getMinZ());
        int maxBlockZ = Math.min(box.maxZ, pos.getMaxZ());

        copyBlocks(states, section, minBlockY, maxBlockY, minBlockZ, maxBlockZ, minBlockX, maxBlockX);
    }

    private void unpackBlockDataZ(BlockState[] states, ClonedChunkSection section) {
        // TODO: Look into a faster copy for this?
        final ChunkSectionPos pos = section.getPosition();

        final int minBlockX = pos.getMinX();
        final int maxBlockX = pos.getMaxX();

        final int minBlockY = pos.getMinY();
        final int maxBlockY = pos.getMaxY();

        final int minBlockZ = pos.getMinZ();
        final int maxBlockZ = pos.getMaxZ();

        // TODO: Can this be optimized?
        copyBlocks(states, section, minBlockY, maxBlockY, minBlockZ, maxBlockZ, minBlockX, maxBlockX);
    }

    private static boolean blockBoxContains(BlockBox box, int x, int y, int z) {
        return x >= box.minX &&
                x <= box.maxX &&
                y >= box.minY &&
                y <= box.maxY &&
                z >= box.minZ &&
                z <= box.maxZ;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean isAir(BlockPos pos) {
        BlockState state = this.getBlockState(pos);
        return state.getBlock().getMaterial() == Material.AIR;
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (!blockBoxContains(volume, x, y, z)) {
            return Blocks.AIR.getDefaultState();
        }

        int relX = x - baseX;
        int relY = y - baseY;
        int relZ = z - baseZ;

        return getBlockStateRelative(relX, relY, relZ);
    }

    public BlockState getBlockStateRelative(int x, int y, int z) {
        // NOTE: Not bounds checked. We assume ChunkRenderRebuildTask is the only function using this
        int sectionIdx = getLocalSectionIndex(x >> 4, y >> 4, z >> 4);
        int blockIdx = getLocalBlockIndex(x & 15, y & 15, z & 15);

        return blockStatesArrays[sectionIdx][blockIdx];
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        if (!blockBoxContains(this.volume, x, y, z)) {
            return null;
        }

        int relX = x - this.baseX;
        int relY = y - this.baseY;
        int relZ = z - this.baseZ;

        int sectionIdx = getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4);

        return this.sections[sectionIdx].getBlockEntity(relX & 15, relY & 15, relZ & 15);
    }

    @Override
    public int getLight(BlockPos pos, int ambientLight) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (y < 0 || y >= 256 || x < -30_000_000 || z < -30_000_000 || x >= 30_000_000 || z >= 30_000_000) {
            return (defaultLightValues[0] << 20) | (ambientLight << 4);
        }

        int skyBrightness = getLightFromNeighborsFor(LightType.SKY, pos);
        int blockBrightness = getLightFromNeighborsFor(LightType.BLOCK, pos);

        if (blockBrightness < ambientLight) {
            blockBrightness = ambientLight;
        }

        return skyBrightness << 20 | blockBrightness << 4;
    }

    private int getLightFor(LightType type, int relX, int relY, int relZ) {
        int sectionIdx = getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4);
        ChunkNibbleArray lightArray = lightArrays[sectionIdx][type.ordinal()];
        if (lightArray == null) {
            // If the array is null, it means the dimension for the current world does not support that light type
            return defaultLightValues[type.ordinal()];
        }

        return lightArray.get(relX & 15, relY & 15, relZ & 15);
    }

    private int getLightFromNeighborsFor(LightType type, BlockPos pos) {
        //if (!world.dimension.hasNoSkylight() && type == LightType.SKY) {
        //    return defaultLightValues[LightType.SKY.ordinal()];
        //}

        int relX = pos.getX() - baseX;
        int relY = pos.getY() - baseY;
        int relZ = pos.getZ() - baseZ;

        BlockState state = getBlockStateRelative(relX, relY, relZ);

        if (!state.getBlock().usesNeighbourLight()) {
            return getLightFor(type, relX, relY, relZ);
        } else {
            int west = getLightFor(type, relX - 1, relY, relZ);
            int east = getLightFor(type, relX + 1, relY, relZ);
            int up = getLightFor(type, relX, relY + 1, relZ);
            int down = getLightFor(type, relX, relY - 1, relZ);
            int north = getLightFor(type, relX, relY, relZ + 1);
            int south = getLightFor(type, relX, relY, relZ - 1);

            if (east > west) {
                west = east;
            }

            if (up > west) {
                west = up;
            }

            if (down > west) {
                west = down;
            }

            if (north > west) {
                west = north;
            }

            if (south > west) {
                west = south;
            }

            return west;
        }
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        int x = (pos.getX() - baseX) >> 4;
        int z = (pos.getZ() - baseZ) >> 4;

        ClonedChunkSection section = sections[getLocalChunkIndex(x, z)];

        if (section != null) {
            return section.getBiomeForNoiseGen(pos.getX() & 15, pos.getZ() & 15);
        }

        return Biome.PLAINS;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int getBlockTint(BlockPos pos, BiomeColors.ColorProvider resolver) {
        if (!blockBoxContains(volume, pos.getX(), pos.getY(), pos.getZ())) {
            return resolver.getColorAtPos(Biome.PLAINS, pos);
        }

        BiomeColorCache cache;

        if (prevColorResolver == resolver) {
            cache = prevColorCache;
        } else {
            cache = biomeColorCaches.get(resolver);

            if (cache == null) {
                biomeColorCaches.put(resolver, cache = new BiomeColorCache(resolver, this));
            }

            prevColorResolver = resolver;
            prevColorCache = cache;
        }

        return cache.getBlendedColor(pos);
    }

    @Override
    public int getStrongRedstonePower(BlockPos pos, Direction direction) {
        BlockState state = getBlockState(pos);
        return state.getBlock().getStrongRedstonePower(this, pos, state, direction);
    }

    @Override
    public LevelGeneratorType getGeneratorType() {
        return worldType;
    }

    /**
     * Gets or computes the biome at the given global coordinates.
     */
    public Biome getBiome(int x, int y, int z) {
        int relX = x - baseX;
        int relY = y - baseY;
        int relZ = z - baseZ;

        int idx = getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4);

        if (idx < 0 || idx >= biomeCaches.length) {
            return Biome.PLAINS;
        }

        return biomeCaches[idx].getBiome(x, relY >> 4, z);
    }

    public float getBrightness(Direction direction, boolean shaded) {
        if (!shaded) {
            return world.dimension.hasNoSkylight() ? 0.9f : 1.0f;
        }
        if(direction == Direction.DOWN){
            return .5f;
        }
        else if(direction == Direction.UP){
            return 1f;
        }
        else if(direction == Direction.NORTH || direction == Direction.SOUTH){
            return .8f;
        }
        else{
            return .6f;
        }
    }

    // [VanillaCopy] PalettedContainer#toIndex
    public static int getLocalBlockIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    public static int getLocalSectionIndex(int x, int y, int z) {
        return y << TABLE_BITS << TABLE_BITS | z << TABLE_BITS | x;
    }

    public static int getLocalChunkIndex(int x, int z) {
        return z << TABLE_BITS | x;
    }
}
