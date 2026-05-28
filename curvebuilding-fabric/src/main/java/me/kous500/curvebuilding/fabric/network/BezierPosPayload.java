package me.kous500.curvebuilding.fabric.network;

import net.minecraft.util.Identifier;

/**
 * Shared packet identifier for the Bezier position preview channel.
 */
public final class BezierPosPayload {
    public static final Identifier ID = new Identifier("curvebuilding", "bezier-pos");

    private BezierPosPayload() {
    }
}