package me.kous500.curvebuilding.commands.bc;

import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;

/**
 * A walk along a straight line, block by block, in the order that the line goes through them, by the traversal of
 * Amanatides and Woo. It starts in the block that the start of the line is in, and each {@link #next} moves it into
 * the block across the next edge that the line crosses, until the line ends. Places along the line are given as
 * fractions of it, from 0 at its start to 1 at its end.
 *
 * <p>A place on the edge between two blocks is in the one with the higher coordinate, as {@link BlockVector3#floor}
 * rounds it. So a line that ends on an edge stays in the block before it, and a line that starts on an edge and goes
 * towards the lower coordinate crosses it at once, at 0.
 *
 * <p>Where the line crosses two or three edges at the same fraction, through the edge or the corner between blocks, it
 * goes across them one at a time, across x first, then y, then z. The blocks between, which the line only touches, are
 * walked through for no length.
 */
final class BlockWalk {
    /** How far the line goes along each axis. */
    private final double[] delta;
    /** The coordinates of the block it is in. */
    private final int[] cell;
    /** For each axis, 1 or -1, the way the line goes along it, or 0 when it does not. */
    private final int[] step = new int[3];
    /** For each axis, the fraction of the line at which it crosses the next edge across that axis. */
    private final double[] nextEdge = new double[3];
    /** For each axis, the fraction of the line between two edges across that axis. */
    private final double[] betweenEdges = new double[3];
    /** The axis of the edge crossed into the block it is in. */
    private int axis;
    /** The fraction of the line at which it went into the block it is in, 0 for the first. */
    private double entered;

    /**
     * A walk along the line from {@code from} to {@code to}, in the block that {@code from} is in.
     *
     * @param from where the line starts
     * @param to where the line ends
     */
    BlockWalk(Vector3 from, Vector3 to) {
        Vector3 way = to.subtract(from);
        double[] origin = {from.x(), from.y(), from.z()};
        BlockVector3 start = BlockVector3.floor(from);
        delta = new double[]{way.x(), way.y(), way.z()};
        cell = new int[]{start.x(), start.y(), start.z()};
        for (int axis = 0; axis < 3; axis++) {
            if (delta[axis] > 0) {
                step[axis] = 1;
                nextEdge[axis] = (cell[axis] + 1 - origin[axis]) / delta[axis];
                betweenEdges[axis] = 1 / delta[axis];
            } else if (delta[axis] < 0) {
                step[axis] = -1;
                nextEdge[axis] = (cell[axis] - origin[axis]) / delta[axis];
                betweenEdges[axis] = -1 / delta[axis];
            } else {
                nextEdge[axis] = Double.POSITIVE_INFINITY;
                betweenEdges[axis] = Double.POSITIVE_INFINITY;
            }
        }
    }

    /**
     * Moves into the next block that the line goes into.
     *
     * @return false, without moving, when the line ends in the block it is in, also on the edge to the next one
     */
    boolean next() {
        int nextAxis = nextEdge[0] <= nextEdge[1]
                ? (nextEdge[0] <= nextEdge[2] ? 0 : 2)
                : (nextEdge[1] <= nextEdge[2] ? 1 : 2);
        double crossing = nextEdge[nextAxis];
        if (crossing >= 1) return false;

        cell[nextAxis] += step[nextAxis];
        nextEdge[nextAxis] += betweenEdges[nextAxis];
        axis = nextAxis;
        entered = crossing;
        return true;
    }

    /**
     * The block it is in.
     *
     * @return the block
     */
    BlockVector3 block() {
        return BlockVector3.at(cell[0], cell[1], cell[2]);
    }

    /**
     * Where the line went into the block it is in.
     *
     * @return the fraction of the line, from 0 at its start to 1 at its end; 0 in the first block
     */
    double entered() {
        return entered;
    }

    /**
     * Where the line, moved back by {@code moved} along each axis, crosses the edge that this line crossed into the
     * block it is in. Only after {@link #next} has moved it.
     *
     * @param moved how far the line is moved back along each axis, in blocks
     * @return the fraction of the line, from 0 at its start to 1 at its end; outside that range when the moved line
     * crosses the edge before its start or after its end
     */
    double enteredWhenMovedBack(double moved) {
        return entered + moved / delta[axis];
    }
}
