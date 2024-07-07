package me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.writer;

import me.jellysquid.mods.sodium.client.model.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.formats.glyph.GlyphVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.BufferBuilder;

public class GlyphVertexWriterFallback extends VertexWriterFallback implements GlyphVertexSink {
    public GlyphVertexWriterFallback(BufferBuilder consumer) {
        super(consumer);
    }

    @Override
    public void writeGlyph(float x, float y, float z, int color, float u, float v, int light) {
    	BufferBuilder consumer = this.consumer;
        int red = ColorABGR.unpackRed(color);
        int green = ColorABGR.unpackGreen(color);
        int blue = ColorABGR.unpackBlue(color);
        int alpha = ColorABGR.unpackAlpha(color);

        consumer.vertex(x, y, z);
        consumer.color(red, green, blue, alpha);
        consumer.texture(u, v);
        consumer.texture2(light & 0xFFFF, light >> 16 & 0xFFFF);
        consumer.next();
    }
}
