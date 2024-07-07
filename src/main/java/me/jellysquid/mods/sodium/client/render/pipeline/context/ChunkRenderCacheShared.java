package me.jellysquid.mods.sodium.client.render.pipeline.context;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.cache.HashLightDataCache;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderCache;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;

import java.util.Map;

public class ChunkRenderCacheShared extends ChunkRenderCache {
    private static final Map<World, ChunkRenderCacheShared> INSTANCES = new Reference2ObjectOpenHashMap<>();

    private final BlockRenderer blockRenderer;
    private final HashLightDataCache lightCache;

    private ChunkRenderCacheShared(WorldSlice world) {
        MinecraftClient client = MinecraftClient.getInstance();

        this.lightCache = new HashLightDataCache(world);

        BiomeColorBlender biomeColorBlender = this.createBiomeColorBlender();
        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightCache);

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, biomeColorBlender);
    }

    private void resetCache() {
        this.lightCache.clearCache();
    }

    public static ChunkRenderCacheShared getInstance(ClientWorld world) {
        ChunkRenderCacheShared instance = INSTANCES.get(world);

        if (instance == null) {
            throw new IllegalStateException("No global renderer exists");
        }

        return instance;
    }

    public static void destroyRenderContext(ClientWorld world) {
        if (INSTANCES.remove(world) == null) {
            throw new IllegalStateException("No render context exists for world: " + world);
        }
    }

    public static void createRenderContext(ClientWorld world) {
        if (INSTANCES.containsKey(world)) {
            throw new IllegalStateException("Render context already exists for world: " + world);
        }

        INSTANCES.put(world, new ChunkRenderCacheShared(new WorldSlice(world)));
    }

    public static void resetCaches() {
        for (ChunkRenderCacheShared context : INSTANCES.values()) {
            context.resetCache();
        }
    }
}
