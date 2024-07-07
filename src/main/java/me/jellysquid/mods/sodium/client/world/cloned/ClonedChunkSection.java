package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.jellysquid.mods.sodium.client.util.math.ChunkSectionPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class ClonedChunkSection {
    private static final ChunkSection EMPTY_SECTION = new ChunkSection(0, false);

    private final AtomicInteger referenceCount = new AtomicInteger(0);
    private final Short2ObjectMap<BlockEntity> blockEntities = new Short2ObjectOpenHashMap<>();

    @Getter
    private final ClonedChunkSectionCache backingCache;
    private final World world;

    private ChunkSectionPos pos;

    private ChunkSection data;

    private boolean hasSky;
    private Biome[] biomeData;

    @Setter
    @Getter
    private long lastUsedTimestamp = Long.MAX_VALUE;

    public void init(ChunkSectionPos context) {
        pos = context;

        Chunk chunk = world.getChunk(pos.getX(), pos.getZ());

        if (chunk == null) {
            throw new RuntimeException("Couldn't retrieve chunk at " + pos.toChunkPos());
        }

        ChunkSection section = getChunkSection(chunk, pos);

        if (section == null) {
            section = EMPTY_SECTION;
        }

        data = section;
        hasSky = section.getSkyLight() != null;
        biomeData = new Biome[chunk.getBiomeArray().length];

        BlockBox box = new BlockBox(pos.getMinX(), pos.getMinY(), pos.getMinZ(), pos.getMaxX(), pos.getMaxY(), pos.getMaxZ());

        blockEntities.clear();

        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            BlockPos entityPos = entry.getKey();

            if (box.contains(entityPos)) {
                blockEntities.put(ChunkSectionPos.packLocal(entityPos), entry.getValue());
            }
        }

        BlockPos.Mutable biomePos = new BlockPos.Mutable();
        // Fill biome data
        for (int z = pos.getMinZ(); z <= pos.getMaxZ(); z++) {
            for (int x = pos.getMinX(); x <= pos.getMaxX(); x++) {
                biomePos.setPosition(x, 100, z);
                biomeData[((z & 15) << 4) | (x & 15)] = world.getBiome(biomePos);
            }
        }
    }

    public BlockState getBlockState(int x, int y, int z) {
        return data.getBlockState(x, y, z);
    }

    public Biome getBiomeForNoiseGen(int x, int z) {
        return biomeData[x | z << 4];
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        return blockEntities.get(packLocal(x, y, z));
    }

    public ChunkSectionPos getPosition() {
        return this.pos;
    }

    public ChunkNibbleArray getLightArray(LightType type) {
        if (type == LightType.SKY) {
            return (!world.dimension.hasNoSkylight() && hasSky) ? data.getSkyLight() : null;
        }
        return data.getBlockLight();
    }

    public int getLightLevel(int x, int y, int z, LightType type) {
        ChunkNibbleArray lightArray = type == LightType.BLOCK ? data.getBlockLight() : data.getSkyLight();
        return lightArray != null ? lightArray.get(x, y, z) : type.defaultValue;
    }

    private static ChunkSection getChunkSection(Chunk chunk, ChunkSectionPos pos) {
        ChunkSection section = null;

        if (!isOutsideBuildHeight(ChunkSectionPos.getBlockCoord(pos.getY()))) {
            section = chunk.getBlockStorage()[pos.getY()];
        }

        return section;
    }

    private static boolean isOutsideBuildHeight(int y) {
        return y < 0 || y >= 256;
    }

    public void acquireReference() {
        referenceCount.incrementAndGet();
    }

    public boolean releaseReference() {
        return referenceCount.decrementAndGet() <= 0;
    }

    /**
     * @param x The local x-coordinate
     * @param y The local y-coordinate
     * @param z The local z-coordinate
     * @return An index which can be used to key entities or blocks within a chunk
     */
    private static short packLocal(int x, int y, int z) {
        return (short) (x << 8 | z << 4 | y);
    }
}
