package me.jellysquid.mods.sodium.common.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public class CameraUtil {
    public static Vec3d getCameraPosition(float partialTicks) {
        return MinecraftClient.getInstance().getCameraEntity().getCameraPosVec(partialTicks);
    }
}
