package me.jellysquid.mods.sodium.mixin.core.frustum;

import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import net.minecraft.client.render.CullingCameraView;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CullingCameraView.class)
public abstract class MixinFrustum implements FrustumExtended {
    @Shadow public abstract boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    @Override
    public boolean fastAabbTest(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * @author XFactHD (ported by embeddedt)
     * @reason Avoid passing infinite extents box into optimized frustum code.
     * This is a port of <a href="https://github.com/MinecraftForge/MinecraftForge/pull/9407">PR #9407</a>
     */
    @Overwrite
    public boolean isBoxInFrustum(Box box) {
        // todo(oldium) can we perhaps add forge patches for this?
        //if(box.equals(TileEntity.INFINITE_EXTENT_AABB))
        //    return true;
        return this.isBoxInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
}
