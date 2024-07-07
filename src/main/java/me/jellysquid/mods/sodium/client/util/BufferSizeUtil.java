package me.jellysquid.mods.sodium.client.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.render.RenderLayer;

// Values was taken from RegionRenderCacheBuilder
public class BufferSizeUtil {
    public static final Map<RenderLayer, Integer> BUFFER_SIZES = new HashMap<>();

    static {
        BUFFER_SIZES.put(RenderLayer.SOLID, 0x200000);
        BUFFER_SIZES.put(RenderLayer.CUTOUT, 0x20000);
        BUFFER_SIZES.put(RenderLayer.CUTOUT_MIPPED, 0x20000);
        BUFFER_SIZES.put(RenderLayer.TRANSLUCENT, 0x40000);
    }
}
