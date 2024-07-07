package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.world.SodiumBlockAccess;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = BiomeColors.class, priority = 1200)
public class MixinBiomeColors {
    /**
     * @author embeddedt
     * @reason reduce allocation rate, use Sodium's biome cache, use configurable biome blending
     */
    @Overwrite
    private static int getColor(BlockView blockAccess, BlockPos pos, BiomeColors.ColorProvider colorResolver) {
        if (blockAccess instanceof SodiumBlockAccess) {
            // Use Sodium's more efficient biome cache
            return ((SodiumBlockAccess) blockAccess).getBlockTint(pos, colorResolver);
        }
        int radius = SodiumClientMod.options().quality.biomeBlendRadius;
        if (radius == 0) {
            return colorResolver.getColorAtPos(blockAccess.getBiome(pos), pos);
        } else {
            int blockCount = (radius * 2 + 1) * (radius * 2 + 1);

            int i = 0;
            int j = 0;
            int k = 0;

            BlockPos.Mutable mutablePos = new BlockPos.Mutable();

            for (int z = -radius; z <= radius; z++) {
                for (int x = -radius; x <= radius; x++) {
                    mutablePos.setPosition(pos.getX() + x, pos.getY(), pos.getZ() + z);
                    int l = colorResolver.getColorAtPos(blockAccess.getBiome(mutablePos), mutablePos);
                    i += (l & 0xff0000) >> 16;
                    j += (l & 0xff00) >> 8;
                    k += l & 0xff;
                }
            }

            return (i / blockCount & 0xff) << 16 | (j / blockCount & 0xff) << 8 | k / blockCount & 0xff;
        }
    }
}
