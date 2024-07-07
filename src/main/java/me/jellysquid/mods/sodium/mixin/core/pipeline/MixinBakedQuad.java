package me.jellysquid.mods.sodium.mixin.core.pipeline;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.jellysquid.mods.sodium.client.util.ModelQuadUtil.*;

@Mixin(BakedQuad.class)
public class MixinBakedQuad implements ModelQuadView {
    @Shadow @Final protected int colorIndex;
    @Shadow @Final protected int[] vertexData;
    @Shadow @Final protected Direction direction;

    @Unique
    protected int cachedFlags;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(int[] vertexDataIn, int tintIndexIn, Direction faceIn, CallbackInfo ci) {
        cachedFlags = ModelQuadFlags.getQuadFlags(this, direction);
    }

    public int vertexOffset(int vertexIndex) {
        return vertexIndex * VERTEX_SIZE;
    }

    @Override
    public float getX(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + POSITION_INDEX]);
    }

    @Override
    public float getY(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + POSITION_INDEX + 1]);
    }

    @Override
    public float getZ(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + POSITION_INDEX + 2]);
    }


    @Override
    public Sprite rubidium$getSprite() {
        return null;
    }


    @Override
    public int getColor(int idx) {
        if(vertexOffset(idx) + COLOR_INDEX < vertexData.length) {
            return this.vertexData[vertexOffset(idx) + COLOR_INDEX];
        }
        else {
            return 0xffffffff;//vertexData.length;
        }
    }

    @Override
    public float getTexU(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + TEXTURE_INDEX]);
    }

    @Override
    public float getTexV(int idx) {
        return Float.intBitsToFloat(this.vertexData[vertexOffset(idx) + TEXTURE_INDEX + 1]);
    }

    @Override
    public int getFlags() {
        return this.cachedFlags;
    }

    @Override
    public int getNormal(int idx) {
        return this.vertexData[vertexOffset(idx) + NORMAL_INDEX];
    }

    @Override
    public int getColorIndex() {
        return this.colorIndex;
    }
}
