package me.jellysquid.mods.sodium.client.model.vertex.formats.quad;

import me.jellysquid.mods.sodium.client.model.vertex.VertexSink;
import me.jellysquid.mods.sodium.client.util.math.MatrixStack;
import me.jellysquid.mods.sodium.client.util.math.MatrixUtil;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

public interface QuadVertexSink extends VertexSink {

    VertexFormat VERTEX_FORMAT = VertexFormats.PARTICLE;

    /**
     * Writes a quad vertex to this sink.
     *
     * @param x The x-position of the vertex
     * @param y The y-position of the vertex
     * @param z The z-position of the vertex
     * @param color The ABGR-packed color of the vertex
     * @param u The u-texture of the vertex
     * @param v The y-texture of the vertex
     * @param light The packed light-map coordinates of the vertex
     * @param overlay The packed overlay-map coordinates of the vertex
     * @param normal The 3-byte packed normal vector of the vertex
     */
    void writeQuad(float x, float y, float z, int color, float u, float v, int light, int overlay, int normal);

    /**
     * Writes a quad vertex to the sink, transformed by the given matrices.
     *
     * @param matrices The matrices to transform the vertex's position and normal vectors by
     */
    default void writeQuad(MatrixStack.Entry matrices, float x, float y, float z, int color, float u, float v, int light, int overlay, int normal) {
        Matrix4f matrix = matrices.getModel();

        float x2 = MatrixUtil.transformVecX(matrix, x, y, z);
        float y2 = MatrixUtil.transformVecY(matrix, x, y, z);
        float z2 = MatrixUtil.transformVecZ(matrix, x, y, z);

        int norm = MatrixUtil.transformPackedNormal(normal, matrices.getNormal());

        this.writeQuad(x2, y2, z2, color, u, v, light, overlay, norm);
    }
}
