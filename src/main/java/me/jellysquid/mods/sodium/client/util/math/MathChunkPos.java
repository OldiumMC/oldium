package me.jellysquid.mods.sodium.client.util.math;

public class MathChunkPos {
    public static int getX(long packedX) {
        return (int) (packedX & 4294967295L);
    }

    public static int getZ(long packedZ) {
        return (int) (packedZ >>> 32 & 4294967295L);
    }
}
