package me.jellysquid.mods.sodium.client.world.cloned;

import me.jellysquid.mods.sodium.client.util.math.ChunkSectionPos;
import net.minecraft.util.math.BlockBox;

public record ChunkRenderContext(ChunkSectionPos origin, ClonedChunkSection[] sections, BlockBox volume) {
    public void releaseResources() {
        for (ClonedChunkSection section : sections) {
            if (section != null)
                section.getBackingCache().release(section);
        }
    }
}
