package me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.writer;

import me.jellysquid.mods.sodium.client.model.vertex.fallback.VertexWriterFallback;
import me.jellysquid.mods.sodium.client.model.vertex.formats.screen_quad.BasicScreenQuadVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import net.minecraft.client.render.BufferBuilder;

public class BasicScreenQuadVertexWriterFallback extends VertexWriterFallback implements BasicScreenQuadVertexSink {
    public BasicScreenQuadVertexWriterFallback(BufferBuilder consumer) {
        super(consumer);
    }

    @Override
    public void writeQuad(float x, float y, float z, int color) {
    	BufferBuilder consumer = this.consumer;
        consumer.vertex(x, y, z);
        consumer.color(ColorABGR.unpackRed(color), ColorABGR.unpackGreen(color), ColorABGR.unpackBlue(color), ColorABGR.unpackAlpha(color));
        consumer.next();
    }
}
