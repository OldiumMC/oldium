package me.jellysquid.mods.sodium.mixin.features.entity.smooth_lighting;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.light.EntityLighter;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.PaintingEntityRenderer;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingEntityRenderer.class)
public abstract class MixinPaintingEntityRenderer extends EntityRenderer<PaintingEntity> {

    @Unique
    private PaintingEntity entity;

    @Unique
    private float tickDelta;

    protected MixinPaintingEntityRenderer(EntityRenderDispatcher renderManager) {
        super(renderManager);
    }

    @Inject(method = "render(Lnet/minecraft/entity/decoration/painting/PaintingEntity;DDDFF)V", at = @At(value = "HEAD"))
    public void oldium$preRender(PaintingEntity paintingEntity, double x, double y, double z, float p_76986_8_, float partialTicks, CallbackInfo ci) {
        entity = paintingEntity;
        tickDelta = partialTicks;
    }

    @Redirect(method = "method_1576", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getLight(Lnet/minecraft/util/math/BlockPos;I)I"))
    public int oldium$redirectLightmapCoord(World world, BlockPos pos, int type) {
        if (SodiumClientMod.options().quality.smoothLighting == SodiumGameOptions.LightingQuality.HIGH && entity != null) {
            return EntityLighter.getBlendedLight(entity, tickDelta);
        } else {
            return world.getLight(pos, type);
        }
    }

}