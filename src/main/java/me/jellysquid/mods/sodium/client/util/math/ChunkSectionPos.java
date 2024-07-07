package me.jellysquid.mods.sodium.client.util.math;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

public class ChunkSectionPos extends Vec3i {
    private ChunkSectionPos(int i, int j, int k) {
        super(i, j, k);
    }

    public static ChunkSectionPos from(int x, int y, int z) {
        return new ChunkSectionPos(x, y, z);
    }

    public static ChunkSectionPos from(BlockPos pos) {
        int x = getSectionCoord(pos.getX());
        int y = getSectionCoord(pos.getY());
        int z = getSectionCoord(pos.getZ());
        return new ChunkSectionPos(x, y, z);
    }

    public static ChunkSectionPos from(ChunkPos chunkPos, int y) {
        return new ChunkSectionPos(chunkPos.x, y, chunkPos.z);
    }

    public static ChunkSectionPos from(Entity entity) {
        BlockPos pos = entity.getBlockPos();
        int x = getSectionCoord(pos.getX());
        int y = getSectionCoord(pos.getY());
        int z = getSectionCoord(pos.getZ());
        return new ChunkSectionPos(x, y, z);
    }

    public static ChunkSectionPos from(long packed) {
        return new ChunkSectionPos(unpackX(packed), unpackY(packed), unpackZ(packed));
    }

    public static long offset(long packed, Direction direction) {
        return offset(packed, direction.getOffsetX(), direction.getOffsetY(), direction.getOffsetZ());
    }

    public static long offset(long packed, int x, int y, int z) {
        return asLong(unpackX(packed) + x, unpackY(packed) + y, unpackZ(packed) + z);
    }

    public static int getSectionCoord(int coord) {
        return coord >> 4;
    }

    public static int getLocalCoord(int coord) {
        return coord & 15;
    }

    public static short packLocal(BlockPos pos) {
        int x = getLocalCoord(pos.getX());
        int y = getLocalCoord(pos.getY());
        int z = getLocalCoord(pos.getZ());
        return (short) (x << 8 | z << 4 | y);
    }

    public static int unpackLocalX(short packedLocalPos) {
        return packedLocalPos >>> 8 & 15;
    }

    public static int unpackLocalY(short packedLocalPos) {
        return packedLocalPos & 15;
    }

    public static int unpackLocalZ(short packedLocalPos) {
        return packedLocalPos >>> 4 & 15;
    }

    public static int getBlockCoord(int sectionCoord) {
        return sectionCoord << 4;
    }

    public static int unpackX(long packed) {
        return (int) (packed >> 42);
    }

    public static int unpackY(long packed) {
        return (int) (packed << 44 >> 44);
    }

    public static int unpackZ(long packed) {
        return (int) (packed << 22 >> 42);
    }

    public static long fromBlockPos(long blockPos) {
        BlockPos pos = BlockPos.fromLong(blockPos);
        int x = getSectionCoord(pos.getX());
        int y = getSectionCoord(pos.getY());
        int z = getSectionCoord(pos.getZ());
        return asLong(x, y, z);
    }

    public static long withZeroY(long pos) {
        return pos & 0xfffffffffff00000L;
    }

    public static long asLong(int x, int y, int z) {
        long l = 0L;
        l |= ((long) x & 4194303L) << 42;
        l |= ((long) y & 1048575L);
        l |= ((long) z & 4194303L) << 20;
        return l;
    }

    public int unpackBlockX(short packedLocalPos) {
        return getMinX() + unpackLocalX(packedLocalPos);
    }

    public int unpackBlockY(short packedLocalPos) {
        return getMinY() + unpackLocalY(packedLocalPos);
    }

    public int unpackBlockZ(short packedLocalPos) {
        return getMinZ() + unpackLocalZ(packedLocalPos);
    }

    public BlockPos unpackBlockPos(short packedLocalPos) {
        int x = unpackBlockX(packedLocalPos);
        int y = unpackBlockY(packedLocalPos);
        int z = unpackBlockZ(packedLocalPos);
        return new BlockPos(x, y, z);
    }

    public int getSectionX() {
        return getX();
    }

    public int getSectionY() {
        return getY();
    }

    public int getSectionZ() {
        return getZ();
    }

    public int getMinX() {
        return getSectionX() << 4;
    }

    public int getMinY() {
        return getSectionY() << 4;
    }

    public int getMinZ() {
        return getSectionZ() << 4;
    }

    public int getMaxX() {
        return (getSectionX() << 4) + 15;
    }

    public int getMaxY() {
        return (getSectionY() << 4) + 15;
    }

    public int getMaxZ() {
        return (getSectionZ() << 4) + 15;
    }

    public BlockPos getMinPos() {
        int x = getBlockCoord(getSectionX());
        int y = getBlockCoord(getSectionY());
        int z = getBlockCoord(getSectionZ());
        return new BlockPos(x, y, z);
    }

    public BlockPos getCenterPos() {
        return getMinPos().add(8, 8, 8);
    }

    public ChunkPos toChunkPos() {
        return new ChunkPos(getSectionX(), getSectionZ());
    }

    public long asLong() {
        return asLong(getSectionX(), getSectionY(), getSectionZ());
    }

    public Iterable<BlockPos> streamBlocks() {
        return BlockPos.iterate(new BlockPos(getMinX(), getMinY(), getMinZ()), new BlockPos(getMaxX(), getMaxY(), getMaxZ()));
    }
}