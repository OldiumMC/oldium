package me.jellysquid.mods.sodium.mixin.features.world_ticking;

import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld {
    @Redirect(method = "spawnRandomParticles", at = @At(value = "NEW", target = "java/util/Random"))
    private Random redirectRandomTickRandom() {
        return new XoRoShiRoRandom();
    }
}
