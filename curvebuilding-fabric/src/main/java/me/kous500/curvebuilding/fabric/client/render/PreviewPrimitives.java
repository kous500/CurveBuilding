package me.kous500.curvebuilding.fabric.client.render;

import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects lines and quads in world coordinates and hands them to the backend in the form it draws:
 * coordinates relative to the camera, which keeps float precision far from the origin, and colors from 0 to 1.
 */
final class PreviewPrimitives {
    private final Vec3 cameraPosition;
    private final boolean isThroughWalls;
    // {x1, y1, z1, x2, y2, z2, red, green, blue, alpha}
    private final List<float[]> lines = new ArrayList<>();
    // {x, y, z, red, green, blue, alpha}; every four make one quad
    private final List<float[]> quadVertices = new ArrayList<>();

    PreviewPrimitives(Vec3 cameraPosition, boolean isThroughWalls) {
        this.cameraPosition = cameraPosition;
        this.isThroughWalls = isThroughWalls;
    }

    void line(Color color, Vec3 start, Vec3 end) {
        float[] c = color(color);
        float[] s = relative(start);
        float[] e = relative(end);
        lines.add(new float[]{s[0], s[1], s[2], e[0], e[1], e[2], c[0], c[1], c[2], c[3]});
    }

    void quad(Color color, Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
        float[] rgba = color(color);
        for (Vec3 corner : new Vec3[]{a, b, c, d}) {
            float[] v = relative(corner);
            quadVertices.add(new float[]{v[0], v[1], v[2], rgba[0], rgba[1], rgba[2], rgba[3]});
        }
    }

    void draw(PreviewBackend backend) {
        if (!lines.isEmpty()) {
            backend.drawLines(isThroughWalls, buffer -> {
                for (float[] l : lines) buffer.line(l[0], l[1], l[2], l[3], l[4], l[5], l[6], l[7], l[8], l[9]);
            });
        }
        if (!quadVertices.isEmpty()) {
            backend.drawQuads(isThroughWalls, buffer -> {
                for (float[] v : quadVertices) buffer.quadVertex(v[0], v[1], v[2], v[3], v[4], v[5], v[6]);
            });
        }
    }

    private float[] relative(Vec3 position) {
        return new float[]{
                (float) (position.x - cameraPosition.x),
                (float) (position.y - cameraPosition.y),
                (float) (position.z - cameraPosition.z)
        };
    }

    private static float[] color(Color color) {
        return new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f};
    }
}
