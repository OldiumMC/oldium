package me.jellysquid.mods.sodium.mixin.features.debug;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.util.Formatting;

@Mixin(DebugHud.class)
public abstract class MixinDebugHud {
    @Shadow
    private static long toMiB(long bytes) {
        throw new RuntimeException("mixin");
    }

    @Redirect(method = "getRightText", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;", remap = false))
    private ArrayList<String> redirectRightTextEarly(Object[] elements) {
        ArrayList<String> strings = Lists.newArrayList((String[]) elements);
        strings.add("");
        strings.add(SodiumClientMod.MODNAME + " Renderer");
        strings.add(Formatting.UNDERLINE + getFormattedVersionText());
        strings.add("");
        strings.addAll(getChunkRendererDebugStrings());

        if (SodiumClientMod.options().advanced.ignoreDriverBlacklist) {
            strings.add(Formatting.RED + "(!!) Driver blacklist ignored");
        }

        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);

            if (str.startsWith("Allocated:")) {
                strings.add(i + 1, getNativeMemoryString());

                break;
            }
        }

        return strings;
    }

    @Unique
    private static String getFormattedVersionText() {
        String version = SodiumClientMod.getVersion();
        Formatting color;

        if (version.contains("git.")) {
            color = Formatting.RED;
        } else {
            color = Formatting.GREEN;
        }

        return color + version;
    }

    @Unique
    private static List<String> getChunkRendererDebugStrings() {
        SodiumWorldRenderer renderer = SodiumWorldRenderer.getInstanceNullable();
        if (renderer == null)
            return ImmutableList.of();
        ChunkRenderBackend<?> backend = renderer.getChunkRenderer();

        List<String> strings = new ArrayList<>(5);
        strings.add("Chunk Renderer: " + backend.getRendererName());
        strings.addAll(backend.getDebugStrings());

        return strings;
    }

    @Unique
    private static String getNativeMemoryString() {
        return "Off-Heap: +" + toMiB(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed()) + "MB";
    }
}
