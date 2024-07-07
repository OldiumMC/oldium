package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.RenderType;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.common.util.WorldUtil;
import me.jellysquid.mods.sodium.mixin.features.chunk_rendering.AccessorBlockFluidRenderer;
import me.jellysquid.mods.sodium.mixin.features.chunk_rendering.AccessorBlockRenderDispatcher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.FluidRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.level.LevelGeneratorType;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 * <p>
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkRenderRebuildTask<T extends ChunkGraphicsState> extends ChunkRenderBuildTask<T> {
    private static final RenderLayer[] LAYERS = RenderLayer.values();
    private final ChunkRenderContainer<T> render;

    private final BlockPos offset;

    private final ChunkRenderContext context;

    private Vec3d camera;

    private final boolean translucencySorting;

    public ChunkRenderRebuildTask(ChunkRenderContainer<T> render, ChunkRenderContext context, BlockPos offset) {
        this.render = render;
        this.offset = offset;
        this.context = context;
        this.camera = new Vec3d(0.0F, 0.0F, 0.0F);
        this.translucencySorting = SodiumClientMod.options().advanced.translucencySorting;
    }

    public ChunkRenderRebuildTask<T> withCameraPosition(Vec3d camera) {
        this.camera = camera;
        return this;
    }

    @Override
    public ChunkBuildResult<T> performBuild(ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers, CancellationSource cancellationSource) {
        // COMPATIBLITY NOTE: Oculus relies on the LVT of this method being unchanged, at least in 16.5
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();
        ChunkRenderBounds.Builder bounds = new ChunkRenderBounds.Builder();

        buffers.init(renderData);

        cache.init(this.context);

        WorldSlice slice = cache.getWorldSlice();

        int baseX = this.render.getOriginX();
        int baseY = this.render.getOriginY();
        int baseZ = this.render.getOriginZ();

        BlockPos.Mutable pos = new BlockPos.Mutable();
        BlockPos renderOffset = this.offset;

        try {
            for (int relY = 0; relY < 16; relY++) {
                if (cancellationSource.isCancelled()) {
                    return null;
                }

                for (int relZ = 0; relZ < 16; relZ++) {
                    for (int relX = 0; relX < 16; relX++) {
                        BlockState blockState = slice.getBlockStateRelative(relX + 16, relY + 16, relZ + 16);
                        Block block = blockState.getBlock();

                        // If the block is vanilla air, assume it renders nothing. Don't use isAir because mods
                        // can abuse it for all sorts of things
                        if (block.getMaterial() == Material.AIR) {
                            continue;
                        }

                        pos.setPosition(baseX + relX, baseY + relY, baseZ + relZ);

                        buffers.setRenderOffset(pos.getX() - renderOffset.getX(), pos.getY() - renderOffset.getY(), pos.getZ() - renderOffset.getZ());

                        int renderType = block.getBlockType();
                        if (renderType != RenderType.INVISIBLE) {
                            if (slice.getGeneratorType() != LevelGeneratorType.DEBUG) {
                                blockState = block.getBlockState(blockState, slice, pos);
                            }

                            for (RenderLayer layer : LAYERS) {
                                if (block.getRenderLayerType() != layer) {
                                    continue;
                                }

                                if (renderType == RenderType.MODEL && WorldUtil.toFluidBlock(block) == null) {
                                    BakedModel model = cache.getBlockModels()
                                            .getBakedModel(blockState);

                                    if (cache.getBlockRenderer().renderModel(cache.getLocalSlice(), blockState, pos, model, buffers.get(layer), true)) {
                                        bounds.addBlock(relX, relY, relZ);
                                    }

                                } else if (WorldUtil.toFluidBlock(block) != null) {
                                    //FluidRenderer fluidRenderer = ((AccessorBlockRenderDispatcher) MinecraftClient.getInstance().getBlockRenderManager()).getFluidRenderer();
                                    //fluidRenderer.render(slice, blockState, pos, buffers.get(layer));
                                    if (cache.getFluidRenderer().render(cache.getLocalSlice(), blockState, pos, buffers.get(layer))) {
                                        bounds.addBlock(relX, relY, relZ);
                                    }
                                }
                            }
                        }

                        if (block.hasBlockEntity()) {
                            BlockEntity entity = slice.getBlockEntity(pos);

                            if (entity != null) {
                                BlockEntityRenderer<BlockEntity> renderer = BlockEntityRenderDispatcher.INSTANCE.getRenderer(entity);

                                if (renderer != null) {
                                    renderData.addBlockEntity(entity, !renderer.rendersOutsideBoundingBox());

                                    bounds.addBlock(relX, relY, relZ);
                                }
                            }
                        }

                        if (block.hasTransparency()) {
                            occluder.markClosed(pos);
                        }
                    }
                }
            }
        } catch (CrashException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getReport(), slice, pos);
        } catch (Throwable ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.create(ex, "Encountered exception while building chunk meshes"), slice, pos);
        }

        render.setRebuildForTranslucents(false);
        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass, (float) camera.x - offset.getX(), (float) camera.y - offset.getY(), (float) camera.z - offset.getZ(), this.translucencySorting);

            if (mesh != null) {
                renderData.setMesh(pass, mesh);
                if (this.translucencySorting && pass.isTranslucent())
                    render.setRebuildForTranslucents(true);
            }
        }

        renderData.setOcclusionData(occluder.build());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        return new ChunkBuildResult<>(this.render, renderData.build());
    }

    private CrashException fillCrashInfo(CrashReport report, WorldSlice slice, BlockPos pos) {
        CrashReportSection crashReportSection = report.addElement("Block being rendered", 1);

        BlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {
        }
        CrashReportSection.addBlockInfo(crashReportSection, pos, state);

        crashReportSection.add("Chunk section", render);
        if (context != null) {
            crashReportSection.add("Render context volume", context.volume());
        }

        return new CrashException(report);
    }

    @Override
    public void releaseResources() {
        this.context.releaseResources();
    }
}
