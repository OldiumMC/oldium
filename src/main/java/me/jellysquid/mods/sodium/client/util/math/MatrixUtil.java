package me.jellysquid.mods.sodium.client.util.math;

import me.jellysquid.mods.sodium.client.util.Norm3b;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class MatrixUtil {
    public static float transformVecX(Matrix4f matrix, float x, float y, float z) {
        return (matrix.m00() * x) + (matrix.m01() * y) + (matrix.m02() * z) + (matrix.m03());
    }

    public static float transformVecY(Matrix4f matrix, float x, float y, float z) {
        return (matrix.m10() * x) + (matrix.m11() * y) + (matrix.m12() * z) + (matrix.m13());
    }

    public static float transformVecZ(Matrix4f matrix, float x, float y, float z) {
        return (matrix.m20() * x) + (matrix.m21() * y) + (matrix.m22() * z) + (matrix.m23());
    }

    public static float transformVecX(Matrix3f matrix, float x, float y, float z) {
        return matrix.m00() * x + matrix.m01() * y + matrix.m02() * z;
    }

    public static float transformVecY(Matrix3f matrix, float x, float y, float z) {
        return matrix.m10() * x + matrix.m11() * y + matrix.m12() * z;
    }

    public static float transformVecZ(Matrix3f matrix, float x, float y, float z) {
        return matrix.m20() * x + matrix.m21() * y + matrix.m22() * z;
    }

    public static int transformPackedNormal(int norm, Matrix3f matrix) {
        float normX1 = Norm3b.unpackX(norm);
        float normY1 = Norm3b.unpackY(norm);
        float normZ1 = Norm3b.unpackZ(norm);

        float normX2 = transformVecX(matrix, normX1, normY1, normZ1);
        float normY2 = transformVecY(matrix, normX1, normY1, normZ1);
        float normZ2 = transformVecZ(matrix, normX1, normY1, normZ1);

        return Norm3b.pack(normX2, normY2, normZ2);
    }
}
