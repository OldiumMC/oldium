package me.jellysquid.mods.sodium.client.world.cloned;

import me.jellysquid.mods.sodium.client.util.math.ChunkSectionPos;
import net.minecraft.util.math.BlockBox;

public final class ChunkRenderContext{
    ChunkSectionPos origin;
    ClonedChunkSection[] sections;
    BlockBox volume;

    public ChunkRenderContext(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public ChunkSectionPos origin() {
        return origin;
    }

    public ClonedChunkSection[] sections() {
        return sections;
    }

    public BlockBox volume() {
        return volume;
    }

    public void releaseResources() {
        for (ClonedChunkSection section : sections) {
            if (section != null)
                section.getBackingCache().release(section);
        }
    }
}
