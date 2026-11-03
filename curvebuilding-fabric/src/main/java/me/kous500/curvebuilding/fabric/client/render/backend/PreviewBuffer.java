package me.kous500.curvebuilding.fabric.client.render.backend;

/**
 * Receives the preview geometry, in coordinates relative to the camera.
 * How the vertices are written depends on the Minecraft version; see {@link PreviewBackend}.
 */
public interface PreviewBuffer {
    /**
     * Adds a line between two points.
     *
     * @param x1 the x of the first point
     * @param y1 the y of the first point
     * @param z1 the z of the first point
     * @param x2 the x of the second point
     * @param y2 the y of the second point
     * @param z2 the z of the second point
     * @param red the red of the color, from 0 to 1
     * @param green the green of the color, from 0 to 1
     * @param blue the blue of the color, from 0 to 1
     * @param alpha the opacity, from 0 to 1
     */
    void line(
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float red,
            float green,
            float blue,
            float alpha);

    /**
     * Adds one corner of a quad. Every four calls make one quad.
     *
     * @param x the x of the corner
     * @param y the y of the corner
     * @param z the z of the corner
     * @param red the red of the color, from 0 to 1
     * @param green the green of the color, from 0 to 1
     * @param blue the blue of the color, from 0 to 1
     * @param alpha the opacity, from 0 to 1
     */
    void quadVertex(float x, float y, float z, float red, float green, float blue, float alpha);
}
