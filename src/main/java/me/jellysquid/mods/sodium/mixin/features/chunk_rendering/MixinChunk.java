package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LayeredBiomeSource;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public class MixinChunk {
    @Final
    @Shadow
    private World world;

    @Final
    @Shadow
    private byte[] biomeArray;

    @Shadow
    @Final
    public int chunkX, chunkZ;

    @Inject(method = "method_3895", at = @At("RETURN"))
    private void oldium$populateBiomes(CallbackInfo ci) {
        if (world.isClient && !MinecraftClient.getInstance().isInSingleplayer()) {
            LayeredBiomeSource manager = world.getBiomeSource();
            BlockPos.Mutable pos = new BlockPos.Mutable();
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int idx = (z << 4) + x;
                    if ((biomeArray[idx] & 255) != 255) continue;

                    pos.setPosition((chunkX << 4) + x, 0, (chunkZ << 4) + z);

                    Biome generated = manager.getBiomeAt(pos);
                    biomeArray[idx] = (byte) (generated.id & 255);
                }
            }
        }
    }
}
