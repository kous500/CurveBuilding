package me.kous500.curvebuilding.fabric.client.render;

/**
 * Receives the preview geometry, in coordinates relative to the camera.
 * How the vertices are written depends on the Minecraft version; see {@link PreviewBackend}.
 */
public interface PreviewBuffer {
    /**
     * Adds a line between two points.
     */
    void line(float x1, float y1, float z1, float x2, float y2, float z2, float red, float green, float blue, float alpha);

    /**
     * Adds one corner of a quad. Every four calls make one quad.
     */
    void quadVertex(float x, float y, float z, float red, float green, float blue, float alpha);
}
