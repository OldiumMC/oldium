package me.jellysquid.mods.sodium.mixin.features.render_layer.leaves;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.Block;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LeavesBlock.class)
public class MixinLeavesBlock extends Block {
    public MixinLeavesBlock(Material blockMaterialIn, MaterialColor blockMapColorIn) {
        super(blockMaterialIn, blockMapColorIn);
    }

    @ModifyVariable(method = "setGraphics", at = @At("HEAD"), argsOnly = true, index = 1)
    private boolean getSodiumLeavesQuality(boolean fancy) {
        return SodiumClientMod.options().quality.leavesQuality.isFancy(fancy);
    }
}