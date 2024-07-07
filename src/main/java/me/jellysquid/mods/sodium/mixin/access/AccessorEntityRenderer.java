package me.jellysquid.mods.sodium.mixin.access;

import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface AccessorEntityRenderer {
    @Accessor
    float getFogRed();

    @Accessor
    float getFogGreen();

    @Accessor
    float getFogBlue();
}
