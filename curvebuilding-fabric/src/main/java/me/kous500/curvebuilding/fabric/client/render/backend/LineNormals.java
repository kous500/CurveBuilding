package me.kous500.curvebuilding.fabric.client.render.backend;

/**
 * Normal of a line for the vanilla line shader, which expands each line along its direction.
 */
final class LineNormals {
    private LineNormals() {
    }

    static float[] normal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) return new float[]{0, 1, 0};
        return new float[]{dx / length, dy / length, dz / length};
    }
}
