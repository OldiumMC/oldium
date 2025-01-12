package me.jellysquid.mods.sodium.client.render;

import java.util.Map;
import java.util.Set;
import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.compat.FogHelper;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.oneshot.ChunkRenderBackendOneshot;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheShared;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
import me.jellysquid.mods.sodium.common.util.ListUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.CullingCameraView;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profiler;
import org.lwjgl.opengl.GL11;

/**
 * Provides an extension to vanilla's {@link net.minecraft.client.render.WorldRenderer}.
 */
public class SodiumWorldRenderer implements ChunkStatusListener {
    private static SodiumWorldRenderer instance;

    private final MinecraftClient client;

    private ClientWorld world;
    private int renderDistance;

    private double lastCameraX, lastCameraY, lastCameraZ;
    private double lastCameraPitch, lastCameraYaw;
    private float lastFogDistance;

    private boolean useEntityCulling;

    private final LongSet loadedChunkPositions = new LongOpenHashSet();
    private final Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    /**
     * -- GETTER --
     *
     * @return The frustum of the current player's camera used to cull chunks
     */
    @Getter
    private CullingCameraView frustum;
    private ChunkRenderManager<?> chunkRenderManager;
    private BlockRenderPassManager renderPassManager;
    private ChunkRenderBackend<?> chunkRenderBackend;

    /**
     * Instantiates Sodium's world renderer. This should be called at the time of the world renderer initialization.
     */
    public static SodiumWorldRenderer create() {
        if (instance == null) {
            instance = new SodiumWorldRenderer(MinecraftClient.getInstance());
        }

        return instance;
    }

    /**
     * @return The current instance of this type
     * @throws IllegalStateException If the renderer has not yet been created
     */
    public static SodiumWorldRenderer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Renderer not initialized");
        }

        return instance;
    }

    /**
     * @return The current instance of the Sodium terrain renderer, or null if the renderer is not active
     */
    public static SodiumWorldRenderer getInstanceNullable() {
        return instance;
    }

    private SodiumWorldRenderer(MinecraftClient client) {
        this.client = client;
    }

    public void setWorld(ClientWorld world) {
        // Check that the world is actually changing
        if (this.world == world) {
            return;
        }

        // If we have a world is already loaded, unload the renderer
        if (this.world != null) {
            this.unloadWorld();
        }

        // If we're loading a new world, load the renderer
        if (world != null) {
            this.loadWorld(world);
        }
    }

    private void loadWorld(ClientWorld world) {
        this.world = world;

        ChunkRenderCacheShared.createRenderContext(this.world);

        this.initRenderer();

        ((ChunkStatusListenerManager) world.getChunkProvider()).setListener(this);
    }

    private void unloadWorld() {
        ChunkRenderCacheShared.destroyRenderContext(this.world);

        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        this.loadedChunkPositions.clear();
        this.globalBlockEntities.clear();

        this.world = null;
    }

    /**
     * @return The number of chunk renders which are visible in the current camera's frustum
     */
    public int getVisibleChunkCount() {
        return this.chunkRenderManager.getVisibleChunkCount();
    }

    /**
     * Notifies the chunk renderer that the graph scene has changed and should be re-computed.
     */
    public void scheduleTerrainUpdate() {
        // BUG: seems to be called before init
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.markDirty();
        }
    }

    /**
     * @return True if no chunks are pending rebuilds
     */
    public boolean isTerrainRenderComplete() {
        return this.chunkRenderManager.isBuildComplete();
    }

    /**
     * Called prior to any chunk rendering in order to update necessary state.
     */
    public void updateChunks(CullingCameraView frustum, float ticks, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.frustum = frustum;

        this.useEntityCulling = SodiumClientMod.options().advanced.useEntityCulling;

        if (this.client.options.viewDistance != this.renderDistance) {
            this.reload();
        }

        Profiler profiler = this.client.profiler;
        profiler.push("camera_setup");

        Entity viewEntity = this.client.getCameraEntity();

        if (viewEntity == null) {
            throw new IllegalStateException("Client instance has no active render entity");
        }

        double x = viewEntity.prevTickX + (viewEntity.x - viewEntity.prevTickX) * ticks;
        double y = viewEntity.prevTickY + (viewEntity.y - viewEntity.prevTickY) * ticks + (double) viewEntity.getEyeHeight();
        double z = viewEntity.prevTickZ + (viewEntity.z - viewEntity.prevTickZ) * ticks;

        this.chunkRenderManager.setCameraPosition(x, y, z);

        float fogDistance = FogHelper.getFogCutoff();

        boolean dirty = x != this.lastCameraX || y != this.lastCameraY ||
                z != this.lastCameraZ || (double) viewEntity.pitch != this.lastCameraPitch |
                (double) viewEntity.yaw != this.lastCameraYaw;

        if (dirty) {
            this.chunkRenderManager.markDirty();
        }

        this.lastCameraX = x;
        this.lastCameraY = y;
        this.lastCameraZ = z;
        this.lastCameraPitch = viewEntity.pitch;
        this.lastCameraYaw = viewEntity.yaw;
        this.lastFogDistance = fogDistance;

        profiler.swap("chunk_update");

        this.chunkRenderManager.updateChunks();

        if (!hasForcedFrustum && chunkRenderManager.isDirty()) {
            profiler.swap("chunk_graph_rebuild");

            this.chunkRenderManager.update(ticks, (FrustumExtended) frustum, frame, spectator);
        }

        profiler.swap("visible_chunk_tick");

        this.chunkRenderManager.tickVisibleRenders();

        profiler.pop();

        // TODO distance checking option
        //Entity.setRenderDistanceWeight(MathHelper.clamp_double((double) this.client.gameSettings.renderDistanceChunks / 8.0D, 1.0D, 2.5D) * 2000);
    }

    /**
     * Performs a render pass for the given {@link Integer} and draws all visible chunks for it.
     */
    public void drawChunkLayer(RenderLayer renderLayer, double x, double y, double z) {
        BlockRenderPass pass = this.renderPassManager.getRenderPassForLayer(renderLayer);

        // TODO startDrawing/endDrawing are handled by 1.12 already
        this.chunkRenderManager.renderLayer(pass, x, y, z);

        GlStateManager.clearColor();
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.initRenderer();
    }

    private void initRenderer() {
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        this.globalBlockEntities.clear();

        RenderDevice device = RenderDevice.INSTANCE;

        this.renderDistance = this.client.options.viewDistance;

        SodiumGameOptions opts = SodiumClientMod.options();

        this.renderPassManager = BlockRenderPassManager.createDefaultMappings();

        final ChunkVertexType vertexFormat;

        if (opts.advanced.useCompactVertexFormat) {
            vertexFormat = DefaultModelVertexFormats.MODEL_VERTEX_HFP;
        } else {
            vertexFormat = DefaultModelVertexFormats.MODEL_VERTEX_SFP;
        }

        this.chunkRenderBackend = createChunkRenderBackend(device, opts, vertexFormat);
        this.chunkRenderBackend.createShaders(device);

        this.chunkRenderManager = new ChunkRenderManager<>(this, this.chunkRenderBackend, this.renderPassManager, this.world, this.renderDistance);
        this.chunkRenderManager.restoreChunks(this.loadedChunkPositions);
    }

    private static ChunkRenderBackend<?> createChunkRenderBackend(RenderDevice device,
                                                                  SodiumGameOptions options,
                                                                  ChunkVertexType vertexFormat) {
        boolean disableBlacklist = SodiumClientMod.options().advanced.ignoreDriverBlacklist;

        if (options.advanced.useChunkMultidraw && MultidrawChunkRenderBackend.isSupported(disableBlacklist)) {
            return new MultidrawChunkRenderBackend(device, vertexFormat);
        } else {
            return new ChunkRenderBackendOneshot(vertexFormat);
        }
    }

    // todo(oldium) changed
    private boolean checkBEVisibility(BlockEntity entity) {
        BlockPos pos = entity.getPos();
        BlockState state = world.getBlockState(pos);
        Box box = entity.getBlock().getCollisionBox(world, entity.getPos(), state);
        return box != null && frustum.isBoxInFrustum(box);
    }

    private void renderTE(BlockEntity tileEntity, int pass, float partialTicks, int damageProgress) {
        // (damageProgress < 0 && !tileEntity.shouldRenderInPass(pass))
        if (!checkBEVisibility(tileEntity))
            return;

        try {
            BlockEntityRenderDispatcher.INSTANCE.renderEntity(tileEntity, partialTicks, damageProgress);
        } catch (RuntimeException e) {
            if (tileEntity.isRemoved()) {
                SodiumClientMod.logger().error("Suppressing crash from invalid tile entity", e);
            } else {
                throw e;
            }
        }
    }

    private void preRenderDamagedBlocks() {
        GlStateManager.blendFuncSeparate(GL11.GL_SRC_COLOR, GL11.GL_DST_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 0.5F);
        GlStateManager.polygonOffset(-1.0F, -10.0F);
        GlStateManager.enablePolyOffset();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableAlphaTest();
        GlStateManager.pushMatrix();
    }

    private void postRenderDamagedBlocks() {
        GlStateManager.disableAlphaTest();
        GlStateManager.polygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolyOffset();
        GlStateManager.enableAlphaTest();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }

    public void renderTileEntities(float partialTicks, Map<Integer, BlockBreakingInfo> damagedBlocks) {
        int pass = 0;
        // todo(oldium) is this necessary for vanilla? it seems to break
        //int pass = MinecraftForgeClient.getRenderPass();
        //TileEntityRendererDispatcher.instance.preDrawBatch();
        for (BlockEntity tileEntity : this.chunkRenderManager.getVisibleBlockEntities()) {
            renderTE(tileEntity, pass, partialTicks, -1);
        }

        for (BlockEntity tileEntity : this.globalBlockEntities) {
            renderTE(tileEntity, pass, partialTicks, -1);
        }
        //TileEntityRendererDispatcher.instance.drawBatch(pass);

        this.preRenderDamagedBlocks();
        for (BlockBreakingInfo destroyProgress : damagedBlocks.values()) {
            BlockPos pos = destroyProgress.getPos();

            if (world.getBlockState(pos).getBlock().hasBlockEntity()) {
                BlockEntity tileEntity = this.world.getBlockEntity(pos);

                if (tileEntity instanceof ChestBlockEntity) {
                    ChestBlockEntity chest = (ChestBlockEntity)tileEntity;
                    if (chest.neighborChestWest != null) {
                        pos = pos.offset(Direction.WEST);
                        tileEntity = world.getBlockEntity(pos);
                    } else if (chest.neighborChestNorth != null) {
                        pos = pos.offset(Direction.NORTH);
                        tileEntity = world.getBlockEntity(pos);
                    }
                }

                if (tileEntity != null) {
                    renderTE(tileEntity, pass, partialTicks, destroyProgress.getStage());
                }
            }
        }
        postRenderDamagedBlocks();
    }

    @Override
    public void onChunkAdded(int x, int z) {
        loadedChunkPositions.add(ChunkPos.getIdFromCoords(x, z));
        chunkRenderManager.onChunkAdded(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        loadedChunkPositions.remove(ChunkPos.getIdFromCoords(x, z));
        chunkRenderManager.onChunkRemoved(x, z);
    }

    public void onChunkRenderUpdated(int x, int y, int z, ChunkRenderData meshBefore, ChunkRenderData meshAfter) {
        ListUtil.updateList(globalBlockEntities, meshBefore.getGlobalBlockEntities(), meshAfter.getGlobalBlockEntities());

        chunkRenderManager.onChunkRenderUpdates(x, y, z, meshAfter);
    }

    private static boolean isInfiniteExtentsBox(Box box) {
        return Double.isInfinite(box.minX) || Double.isInfinite(box.minY) || Double.isInfinite(box.minZ)
                || Double.isInfinite(box.maxX) || Double.isInfinite(box.maxY) || Double.isInfinite(box.maxZ);
    }

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     *
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity) {
        if (!this.useEntityCulling) {
            return true;
        }

        Box box = entity.getBoundingBox();

        // Entities outside the valid world height will never map to a rendered chunk
        // Always render these entities or they'll be culled incorrectly!
        if (box.maxY < 0.5D || box.minY > 255.5D) {
            return true;
        }

        if (isInfiniteExtentsBox(box)) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (isGlowing(entity) || entity.shouldRenderName()) {
            return true;
        }

        int minX = MathHelper.floor(box.minX - 0.5D) >> 4;
        int minY = MathHelper.floor(box.minY - 0.5D) >> 4;
        int minZ = MathHelper.floor(box.minZ - 0.5D) >> 4;

        int maxX = MathHelper.floor(box.maxX + 0.5D) >> 4;
        int maxY = MathHelper.floor(box.maxY + 0.5D) >> 4;
        int maxZ = MathHelper.floor(box.maxZ + 0.5D) >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.chunkRenderManager.isChunkVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isGlowing(Entity entity) {
        return MinecraftClient.getInstance().world.isClient && (entity.getDataTracker().getByte(0) & 1 << 6) != 0;
    }

    // COMPATIBILITY - SpeedRunIGT

    public int getTotalSections() {
        return this.chunkRenderManager.getTotalSections();
    }

    public int getRebuildQueueSize() {
        return this.chunkRenderManager.getRebuildQueueSize();
    }

    public int getImportantRebuildQueueSize() {
        return this.chunkRenderManager.getImportantRebuildQueueSize();
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified block region.
     */
    public void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        this.scheduleRebuildForChunks(minX >> 4, minY >> 4, minZ >> 4, maxX >> 4, maxY >> 4, maxZ >> 4, important);
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified chunk region.
     */
    public void scheduleRebuildForChunks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkY = minY; chunkY <= maxY; chunkY++) {
                for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                    this.scheduleRebuildForChunk(chunkX, chunkY, chunkZ, important);
                }
            }
        }
    }

    /**
     * Schedules a chunk rebuild for the render belonging to the given chunk section position.
     */
    public void scheduleRebuildForChunk(int x, int y, int z, boolean important) {
        this.chunkRenderManager.scheduleRebuild(x, y, z, important);
    }

    public ChunkRenderBackend<?> getChunkRenderer() {
        return this.chunkRenderBackend;
    }
}
