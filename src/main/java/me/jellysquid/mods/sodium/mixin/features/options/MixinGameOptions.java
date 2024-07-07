package me.jellysquid.mods.sodium.mixin.features.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameOptions.class)
public class MixinGameOptions {
    @Shadow
    public int viewDistance;

    @Shadow
    public boolean fancyGraphics;

    /**
     * @author embeddedt
     * @reason Sodium Renderer supports up to 32 chunks
     */
    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void oldium$increaseMaxDistance(CallbackInfo ci) {
        GameOptions.Option.RENDER_DISTANCE.setMaxValue(32);
    }

    /**
     * @author Asek3
     * @reason Implemented cloud rendering option
     */
    @Overwrite
    public int getCloudMode() {
        SodiumGameOptions options = SodiumClientMod.options();

        if (this.viewDistance < 4 || !options.quality.enableClouds) {
            return 0;
        }

        return options.quality.cloudQuality.isFancy(this.fancyGraphics) ? 2 : 1;
    }
}