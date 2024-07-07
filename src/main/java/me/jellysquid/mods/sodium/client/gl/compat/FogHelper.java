package me.jellysquid.mods.sodium.client.gl.compat;

import com.mojang.blaze3d.platform.GlStateManager;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.mixin.access.AccessorBooleanState;
import me.jellysquid.mods.sodium.mixin.access.AccessorEntityRenderer;
import me.jellysquid.mods.sodium.mixin.access.AccessorGlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;

public class FogHelper {
    private static final float FAR_PLANE_THRESHOLD_EXP = (float) Math.log(1.0f / 0.0019f);
    private static final float FAR_PLANE_THRESHOLD_EXP2 = MathHelper.sqrt(FAR_PLANE_THRESHOLD_EXP);

    private static GlStateManager.FogState fogState() {
        return AccessorGlStateManager.getFogState();
    }

    public static float getFogEnd() {
    	return fogState().end;
    }

    public static float getFogStart() {
    	return fogState().start;
    }

    public static float getFogDensity() {
    	return fogState().density;
    }

    /**
     * Retrieves the current fog mode from the fixed-function pipeline.
     */
    public static ChunkFogMode getFogMode() {
        if (!SodiumClientMod.options().quality.enableFog)
            return ChunkFogMode.NONE;

        int mode = fogState().mode;
        
        if(mode == 0 || !((AccessorBooleanState) fogState().capState).getCachedState())
        	return ChunkFogMode.NONE;

        switch (mode) {
            case GL11.GL_EXP2:
            case GL11.GL_EXP:
                return ChunkFogMode.EXP2;
            case GL11.GL_LINEAR:
                return ChunkFogMode.LINEAR;
            default:
                throw new UnsupportedOperationException("Unknown fog mode: " + mode);
        }
    }

    public static float getFogCutoff() {
    	int mode = fogState().mode;

        switch (mode) {
            case GL11.GL_LINEAR:
                return getFogEnd();
            case GL11.GL_EXP:
                return FAR_PLANE_THRESHOLD_EXP / getFogDensity();
            case GL11.GL_EXP2:
                return FAR_PLANE_THRESHOLD_EXP2 / getFogDensity();
            default:
                return 0.0f;
        }
    }
    
    public static float[] getFogColor() {
        AccessorEntityRenderer entityRenderer = (AccessorEntityRenderer)MinecraftClient.getInstance().gameRenderer;
        return new float[]{entityRenderer.getFogRed(), entityRenderer.getFogGreen(), entityRenderer.getFogBlue(), 1.0f};
    }
}
