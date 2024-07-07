package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockBreakingInfo;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.CullingCameraView;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.platform.GlStateManager;
import java.util.List;
import java.util.Map;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Final
    @Shadow
    private Map<Integer, BlockBreakingInfo> blockBreakingInfos;

    @Shadow
    private ClientWorld world;

    @Shadow
    EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    int totalEntityCount;

    @Shadow
    int renderedEntityCount;

    @Shadow
    int hiddenEntityCount;

    @Shadow @Final private MinecraftClient client;

    private SodiumWorldRenderer renderer;

    //@Redirect(method = "loadRenderers", at = @At(value = "FIELD", target = "Lnet/minecraft/client/settings/GameSettings;renderDistanceChunks:I", ordinal = 1))
    //private int nullifyBuiltChunkStorage(GameSettings settings) {
    //    // Do not allow any resources to be allocated
    //    return 0;
    //}

    // setupTerrain

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(MinecraftClient minecraft, CallbackInfo ci) {
        this.renderer = SodiumWorldRenderer.create();
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void onWorldChanged(ClientWorld world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

//    /**
//     * @reason Redirect to our renderer
//     * @author JellySquid
//     */
//    @Overwrite
//    public int getRenderedChunks() {
//        return this.renderer.getVisibleChunkCount();
//    }
//
//    /**
//     * @reason Redirect the check to our renderer
//     * @author JellySquid
//     */
    //@Overwrite
    //public boolean hasNoChunkUpdates() {
    //    return this.renderer.isTerrainRenderComplete();
    //}

   @Inject(method = "scheduleTerrainUpdate", at = @At("RETURN"))
   private void onTerrainUpdateScheduled(CallbackInfo ci) {
       this.renderer.scheduleTerrainUpdate();
   }

    /**
     * @reason Redirect the updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void updateChunks(long p) {
    }

    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int renderLayer(RenderLayer blockLayerIn, double partialTicks, int pass, Entity entityIn) {
        RenderDevice.enterManagedCode();

        DiffuseLighting.disable();

        GlStateManager.activeTexture(GLX.textureUnit);
        GlStateManager.bindTexture(this.client.getSpriteAtlasTexture().getGlId());
        GlStateManager.enableTexture();

        this.client.gameRenderer.enableLightmap();

        double x = entityIn.prevTickX + (entityIn.x - entityIn.prevTickX) * partialTicks;
        double y = entityIn.prevTickY + (entityIn.y - entityIn.prevTickY) * partialTicks;
        double z = entityIn.prevTickZ + (entityIn.z - entityIn.prevTickZ) * partialTicks;

        try {
            this.renderer.drawChunkLayer(blockLayerIn, x, y, z);
        } finally {
            RenderDevice.exitManagedCode();
        }
        this.client.gameRenderer.disableLightmap();

        return 0;
    }

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setupTerrain(Entity entity, double tick, CameraView camera, int frame, boolean spectator) {
        RenderDevice.enterManagedCode();

        boolean hasForcedFrustum = false;
        try {
            this.renderer.updateChunks((CullingCameraView) camera, (float)tick, hasForcedFrustum, frame, spectator);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void updateBlock(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    // The following two redirects force light updates to trigger chunk updates and not check vanilla's chunk renderer
    // flags
    //@Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher;hasNoFreeRenderBuilders()Z"))
    //private boolean alwaysHaveBuilders(ChunkRenderDispatcher instance) {
        //return false;
    //}
//
//    @Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", ordinal = 1))
//    private boolean alwaysHaveNoTasks(Set instance) {
//        return true;
//    }

    @Shadow
    private int renderDistance;

    /**
     * @author Sodium
     * @reason Redirect to our renderer
     */
    @Overwrite
    public void reload() {
        if (this.world == null) return;
        Blocks.LEAVES.setGraphics(this.client.options.fancyGraphics);
        Blocks.LEAVES2.setGraphics(this.client.options.fancyGraphics);
        this.renderDistance = this.client.options.viewDistance;

        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @author Decencies
     * @reason Redirect entities to our renderer
     */
    @Overwrite
    public void renderEntities(Entity player, CameraView camera, float partialTicks) {
        this.world.profiler.push("prepare");
        Entity renderView = client.getCameraEntity();

        BlockEntityRenderDispatcher.INSTANCE
                .updateCamera(world, client.getTextureManager(), client.textRenderer, renderView, partialTicks);

        entityRenderDispatcher
                .updateCamera(world, client.textRenderer, renderView, client.targetedEntity, client.options, partialTicks);

        double renderX = renderView.prevTickX + (renderView.x - renderView.prevTickX) * partialTicks;
        double renderY = renderView.prevTickY + (renderView.y - renderView.prevTickY) * partialTicks;
        double renderZ = renderView.prevTickZ + (renderView.z - renderView.prevTickZ) * partialTicks;
        BlockEntityRenderDispatcher.CAMERA_X = renderX;
        BlockEntityRenderDispatcher.CAMERA_Y = renderY;
        BlockEntityRenderDispatcher.CAMERA_Z = renderZ;

        entityRenderDispatcher.updateCamera(renderX, renderY, renderZ);
        client.gameRenderer.enableLightmap();
        world.profiler.swap("global");
        List<Entity> list = this.world.getLoadedEntities();
        totalEntityCount = list.size();

        Entity effect;
        for(int j = 0; j < world.entities.size(); ++j) {
            effect = world.entities.get(j);
            if (effect.shouldRender(renderX, renderY, renderZ)) {
                entityRenderDispatcher.renderEntity(effect, partialTicks);
            }
        }

        BlockPos.Mutable entityBlockPos = new BlockPos.Mutable();
        // Apply entity distance scaling
        for(Entity entity : world.getLoadedEntities()) {
            // Skip entities that shouldn't render in this pass
            //if(!entity.shouldRenderInPass(pass)) {
            //    continue;
            //}

            // Do regular vanilla checks for visibility
            if (!entity.shouldRender(renderX, renderY, renderZ) && (!entity.hasVehicle() || entity.rider != null)) {
                continue;
            }

            // Check if any corners of the bounding box are in a visible subchunk
            if(!SodiumWorldRenderer.getInstance().isEntityVisible(entity)) {
                continue;
            }

            boolean isSleeping = renderView instanceof LivingEntity && ((LivingEntity) renderView).isSleeping();

            if (!(entity != renderView || client.options.perspective != 0 || isSleeping)) {
                continue;
            }

            entityBlockPos.setPosition((int) entity.x, (int) entity.y, (int) entity.z);

            if (entity.y < 0.0D || entity.y >= 256.0D || this.world.blockExists(entityBlockPos))
            {
                ++this.renderedEntityCount;
                this.entityRenderDispatcher.method_6915(entity, partialTicks, false);
            }
        }

        renderer.renderTileEntities(partialTicks, blockBreakingInfos);

        client.gameRenderer.disableLightmap();
        client.profiler.pop();
    }


    //@Inject(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderHelper;enableStandardItemLighting()V", shift = At.Shift.AFTER, ordinal = 1), cancellable = true)
    //public void sodium$renderTileEntities(Entity entity, ICamera camera, float partialTicks, CallbackInfo ci) {
    //    //this.renderer.renderTileEntities(partialTicks, damagedBlocks);
//
    //    this.mc.entityRenderer.disableLightmap();
    //    this.mc.mcProfiler.endSection();
    //    ci.cancel();
    //}

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String getChunksDebugString() {
        return String.format("C: %s/%s Q: %s+%si", this.renderer.getVisibleChunkCount(), this.renderer.getTotalSections(), this.renderer.getRebuildQueueSize(), this.renderer.getImportantRebuildQueueSize());
    }
}
