package me.jellysquid.mods.sodium.client.world.cloned;

import it.unimi.dsi.fastutil.longs.Long2ReferenceLinkedOpenHashMap;
import me.jellysquid.mods.sodium.client.util.math.ChunkSectionPos;
import net.minecraft.world.World;

import java.util.concurrent.TimeUnit;

public class ClonedChunkSectionCache {
    private static final int MAX_CACHE_SIZE = 512; /* number of entries */
    private static final long MAX_CACHE_DURATION = TimeUnit.SECONDS.toNanos(5); /* number of nanoseconds */

    private final World world;

    private final Long2ReferenceLinkedOpenHashMap<ClonedChunkSection> byPosition = new Long2ReferenceLinkedOpenHashMap<>();
    private long time; // updated once per frame to be the elapsed time since application start

    public ClonedChunkSectionCache(World context) {
        world = context;
        time = getMonotonicTimeSource();
    }

    public synchronized void cleanup() {
        time = getMonotonicTimeSource();
        byPosition.values()
                .removeIf(entry -> time > (entry.getLastUsedTimestamp() + MAX_CACHE_DURATION));
    }

    public synchronized ClonedChunkSection acquire(int x, int y, int z) {
        long key = ChunkSectionPos.asLong(x, y, z);
        ClonedChunkSection section = byPosition.get(key);

        if (section == null) {
            while (byPosition.size() >= MAX_CACHE_SIZE) {
                byPosition.removeFirst();
            }

            section = createSection(x, y, z);
        }

        section.setLastUsedTimestamp(time);

        return section;
    }

    private ClonedChunkSection createSection(int x, int y, int z) {
        ClonedChunkSection section = allocate();

        ChunkSectionPos pos = ChunkSectionPos.from(x, y, z);
        section.init(pos);

        byPosition.putAndMoveToLast(pos.asLong(), section);

        return section;
    }

    public synchronized void invalidate(int x, int y, int z) {
        byPosition.remove(ChunkSectionPos.asLong(x, y, z));
    }

    public void release(ClonedChunkSection section) {
    }

    private ClonedChunkSection allocate() {
        return new ClonedChunkSection(this, this.world);
    }

    private static long getMonotonicTimeSource() {
        // Should be monotonic in JDK 17 on sane platforms...
        return System.nanoTime();
    }
}
