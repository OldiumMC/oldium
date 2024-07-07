package me.jellysquid.mods.sodium.mixin.features.particle.cull;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.CullingCameraView;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleManager.class)
public class MixinParticleManager {

    @Unique
    private CullingCameraView cullingFrustum;

    @Inject(method = {"renderParticles", "method_1299"}, at = @At("HEAD"))
    private void preRenderParticles(Entity entity, float partialTicks, CallbackInfo ci) {
        CullingCameraView frustum = SodiumWorldRenderer.getInstance().getFrustum();
        boolean useCulling = SodiumClientMod.options().advanced.useParticleCulling;

        // Setup the frustum state before rendering particles
        if (useCulling && frustum != null) {
            this.cullingFrustum = frustum;
        } else {
            this.cullingFrustum = null;
        }
    }

    @WrapWithCondition(method = {"renderParticles", "method_1299"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;draw(Lnet/minecraft/client/render/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    private boolean filterParticleList(Particle particle, BufferBuilder f8, Entity f9, float f10, float f11, float f12, float vec3d, float v, float buffer) {
        if(this.cullingFrustum == null) {
            return true;
        }

        Box box = particle.getBoundingBox();

        // Hack: Grow the particle's bounding box in order to work around mis-behaved particles
        return this.cullingFrustum.isBoxInFrustum(box.minX - 1.0D, box.minY - 1.0D, box.minZ - 1.0D, box.maxX + 1.0D, box.maxY + 1.0D, box.maxZ + 1.0D);
    }

}