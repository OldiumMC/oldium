package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ChunkBuilder.class)
public class MixinChunkBuilder {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 2))
    public int oldium$modifyRenderWorkerCount(int constant) {
        return 0;
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 5, ordinal = 1))
    public int oldium$modifyRenderCacheCount(int constant) {
        return 1;
    }
}
