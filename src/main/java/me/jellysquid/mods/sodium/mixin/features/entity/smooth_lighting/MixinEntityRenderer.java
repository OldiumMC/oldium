package me.jellysquid.mods.sodium.mixin.features.entity.smooth_lighting;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.light.EntityLighter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderer<T extends Entity> {
    @Redirect(method = "method_10204", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getLightmapCoordinates(F)I"))
    private int sodium$getBrightnessForRender(Entity self, float partialTicks) {
        if (MinecraftClient.getInstance().options.ao == SodiumGameOptions.LightingQuality.HIGH.ordinal()) {
            return EntityLighter.getBlendedLight(self, partialTicks);
        }

        return self.getLightmapCoordinates(partialTicks);
    }
}