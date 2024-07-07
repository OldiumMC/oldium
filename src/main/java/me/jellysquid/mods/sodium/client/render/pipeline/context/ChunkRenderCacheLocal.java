package me.jellysquid.mods.sodium.client.render.pipeline.context;

import lombok.Getter;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.cache.ArrayLightDataCache;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderCache;
import me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.WorldSliceLocal;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockModelShapes;
import net.minecraft.world.World;

public class ChunkRenderCacheLocal extends ChunkRenderCache {
    private final ArrayLightDataCache lightDataCache;

    @Getter
    private final BlockRenderer blockRenderer;
    @Getter
    private final FluidRenderer fluidRenderer;

    @Getter
    private final BlockModelShapes blockModels;
    @Getter
    private final WorldSlice worldSlice;
    @Getter
    private WorldSliceLocal localSlice;

    public ChunkRenderCacheLocal(MinecraftClient client, World world) {
        this.worldSlice = new WorldSlice(world);
        this.lightDataCache = new ArrayLightDataCache(this.worldSlice);

        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightDataCache);
        BiomeColorBlender biomeColorBlender = this.createBiomeColorBlender();

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, biomeColorBlender);
        this.fluidRenderer = new FluidRenderer(client, lightPipelineProvider, biomeColorBlender);

        this.blockModels = client.getBlockRenderManager().getModels();
    }

    public void init(ChunkRenderContext context) {
        this.lightDataCache.reset(context.origin());
        this.worldSlice.copyData(context);
        // create the new local slice here so that it's unique whenever we copy new data
        // this is passed into mod code, since some depend on the provided BlockRenderView object being unique each time
        this.localSlice = new WorldSliceLocal(this.worldSlice);
    }
}
