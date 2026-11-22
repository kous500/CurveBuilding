package me.kous500.curvebuilding.commands.bc;

import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.math.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a curve into straight pieces for the columns of a cross section to follow. The pieces are given by their
 * vertices on the curve, each with the way across the curve there and the distance along the segment, measured on the
 * curve itself. All columns share the vertices: each follows the straight lines between them, moved to where the
 * column is across the curve and up from it.
 *
 * <p>Along a segment, a part is split in half until three things hold, or it has been split {@link #MAX_SPLITS} times:
 * <ul>
 *   <li>the curve strays at most {@link #TOLERANCE} from the straight line between the ends of the part;</li>
 *   <li>the outermost columns, by turning with the curve, stray about as far from the straight lines between their
 *   own ends, measured as if they turned around one point; a column can stray by both of these together;</li>
 *   <li>the curve bends by at most {@link #MOST_BEND}, so that the length of the part is measured closely.</li>
 * </ul>
 * Where two segments meet at an angle, the cross section turns around the point between them, along straight lines
 * that stray at most {@link #TOLERANCE} from the arc of the outermost columns.
 *
 * <p>The cross section stays upright: the way across the curve is level, and it turns only on the xz plane.
 */
final class CurvePieces {
    /**
     * How far, in blocks, the curve may stray from a straight piece, and about how far the outermost columns may stray
     * from theirs by turning with it.
     */
    static final double TOLERANCE = 0.05;

    /**
     * The most, in radians and in any direction, that the curve turns along one straight piece. Where the curve turns
     * back on itself, its speed falls to nothing with a kink, which {@link BezierSegment#length(double, double)}
     * measures badly unless the piece around it is short.
     */
    private static final double MOST_BEND = Math.toRadians(45);

    /** The most times that a part of a segment is split in half to follow it with straight pieces. */
    private static final int MAX_SPLITS = 20;

    /**
     * A vertex of the straight pieces.
     *
     * @param point where the curve is
     * @param across the way to the left of the curve, on the xz plane and of length 1
     * @param distance how far along the curve the vertex is from the start of the segment
     * @param insideSide for a vertex that turns the cross section at a corner, the side of the curve inside the corner:
     * 1 on the left, -1 on the right; 0 for a vertex along the segment
     */
    record Vertex(Vector3 point, Vector3 across, double distance, int insideSide) {
        /**
         * Whether a column {@code side} blocks to the left of the curve is inside the corner that this vertex turns
         * at, where the column skips the piece to the next vertex.
         */
        boolean isInsideCorner(double side) {
            return insideSide * side > 0;
        }
    }

    /** How far the outermost columns of the cross section are from its middle. */
    private final double outermost;

    /**
     * Pieces for a cross section whose outermost columns are {@code outermost} blocks from its middle.
     *
     * @param outermost how far the outermost columns are from the middle, 0 or more
     */
    CurvePieces(double outermost) {
        this.outermost = outermost;
    }

    /**
     * The vertices of straight pieces along {@code segment}, from its start to its end. A part of the segment is split
     * in half until its control polygon is within {@link #TOLERANCE} of the line between its ends, the outermost
     * columns turn with it by little enough to stray at most that far, and it bends by at most {@link #MOST_BEND}, or
     * it has been split {@link #MAX_SPLITS} times.
     *
     * @param segment the segment
     * @return at least the vertices at its two ends, the last one with the length of the segment as its distance
     */
    List<Vertex> along(BezierSegment segment) {
        List<Vertex> vertices = new ArrayList<>();
        vertices.add(new Vertex(segment.p0(), across(segment.tangent(0)), 0, 0));
        addVertices(segment, segment, 0, 1, 0, vertices);
        return vertices;
    }

    /**
     * Vertices at {@code point}, where a segment starts, that turn the cross section from {@code from}, the way across
     * the end of the segment before, by {@code angle}, towards the way across the start of this one, which the first
     * vertex of the segment has. The outermost columns go around the outside of the corner along straight pieces that
     * stray at most {@link #TOLERANCE} from the arc.
     *
     * @param point where the two segments meet
     * @param from the way across the end of the segment before
     * @param angle the angle to turn by, as {@link #turn} gives it
     * @return the vertices, each at {@code point} with a distance of 0; none when the angle is 0
     */
    List<Vertex> corner(Vector3 point, Vector3 from, double angle) {
        if (angle == 0) return List.of();
        // The angle of the arc of the outermost columns whose chord strays TOLERANCE from it at its middle; a half turn
        // when the columns are that near the middle
        double largestTurn = outermost <= TOLERANCE / 2 ? Math.PI : 2 * Math.acos(1 - TOLERANCE / outermost);
        int pieces = (int) Math.ceil(Math.abs(angle) / largestTurn);
        // A positive angle, the way from +x to +z, turns the curve to the right: the inside of the corner is there
        int insideSide = angle > 0 ? -1 : 1;

        List<Vertex> corner = new ArrayList<>();
        for (int piece = 0; piece < pieces; piece++) {
            double turned = angle * piece / pieces;
            double cos = Math.cos(turned);
            double sin = Math.sin(turned);
            Vector3 across = Vector3.at(from.x() * cos - from.z() * sin, 0, from.x() * sin + from.z() * cos);
            corner.add(new Vertex(point, across, 0, insideSide));
        }
        return corner;
    }

    /**
     * The angle on the xz plane from {@code from} to {@code to}, the shorter way round.
     *
     * @param from the way to turn from
     * @param to the way to turn to
     * @return the angle in radians, from -pi to pi, positive when it turns from +x towards +z
     */
    static double turn(Vector3 from, Vector3 to) {
        return Math.atan2(from.x() * to.z() - from.z() * to.x(), from.x() * to.x() + from.z() * to.z());
    }

    /**
     * The way to the left of {@code direction} on the xz plane, of length 1: towards +x for a curve towards +z. Also +x
     * when {@code direction} does not move on the xz plane, as straight up.
     */
    private static Vector3 across(Vector3 direction) {
        double length = Math.hypot(direction.x(), direction.z());
        if (length == 0) return Vector3.at(1, 0, 0);
        return Vector3.at(direction.z() / length, 0, -direction.x() / length);
    }

    /**
     * Adds to {@code vertices} those of the part of {@code segment} from t = {@code from} to {@code to}, whose control
     * points are those of {@code part}, after the vertex at {@code from}, which {@code vertices} ends with.
     */
    private void addVertices(BezierSegment segment, BezierSegment part, double from, double to, int splits,
            List<Vertex> vertices) {
        if (splits < MAX_SPLITS && !isStraight(part)) {
            double middle = (from + to) / 2;
            List<BezierSegment> halves = part.halves();
            addVertices(segment, halves.getFirst(), from, middle, splits + 1, vertices);
            addVertices(segment, halves.getLast(), middle, to, splits + 1, vertices);
            return;
        }
        double distance = vertices.getLast().distance() + segment.length(from, to);
        vertices.add(new Vertex(segment.point(to), across(segment.tangent(to)), distance, 0));
    }

    /**
     * Whether the columns may follow {@code part} of a segment along one straight piece: its control polygon, which
     * the curve stays within, is within {@link #TOLERANCE} of the line between its ends, and the outermost columns,
     * which turn with the curve around its middle, stray at most that far from the straight piece between their ends.
     * Its length is measured closely only when it also bends by at most {@link #MOST_BEND}.
     */
    private boolean isStraight(BezierSegment part) {
        if (distanceToLine(part.p1(), part.p0(), part.p3()) > TOLERANCE) return false;
        if (distanceToLine(part.p2(), part.p0(), part.p3()) > TOLERANCE) return false;
        Vector3[] sides = sides(part);
        if (largestBend(sides) > MOST_BEND) return false;
        // A column turned by an angle around a point strays from the straight line between its ends by about
        // outermost * angle^2 / 8
        double turn = largestTurn(sides);
        return outermost * turn * turn / 8 <= TOLERANCE;
    }

    /** How far {@code point} is from the straight line from {@code from} to {@code to}, between those ends. */
    private static double distanceToLine(Vector3 point, Vector3 from, Vector3 to) {
        Vector3 line = to.subtract(from);
        double lengthSq = line.lengthSq();
        double along = lengthSq == 0 ? 0 : point.subtract(from).dot(line) / lengthSq;
        along = Math.clamp(along, 0, 1);
        return point.distance(from.add(line.multiply(along)));
    }

    /**
     * The three sides of the control polygon of {@code part}, in order. The way the curve goes is always a sum of
     * them, weighted by the quadratic Bernstein polynomials, so it turns at most as much as they do.
     */
    private static Vector3[] sides(BezierSegment part) {
        return new Vector3[]{
                part.p1().subtract(part.p0()), part.p2().subtract(part.p1()), part.p3().subtract(part.p2())
        };
    }

    /**
     * The most, in radians on the xz plane, that the curve turns along a part whose control polygon has
     * {@code sides}: at most the angle between them. Sides straight up or down do not turn it.
     */
    private static double largestTurn(Vector3[] sides) {
        Vector3 first = null;
        double least = 0;
        double most = 0;
        for (Vector3 side : sides) {
            if (side.x() == 0 && side.z() == 0) continue;
            if (first == null) {
                first = side;
                continue;
            }
            double turn = turn(first, side);
            least = Math.min(least, turn);
            most = Math.max(most, turn);
        }
        return most - least;
    }

    /**
     * The most, in radians and in any direction, that the curve bends along a part whose control polygon has
     * {@code sides}: at most the largest angle between two of them. Sides of length 0, at an end without a handle, do
     * not bend it.
     */
    private static double largestBend(Vector3[] sides) {
        double most = 0;
        for (int i = 0; i < sides.length; i++) {
            for (int j = i + 1; j < sides.length; j++) {
                if (sides[i].lengthSq() == 0 || sides[j].lengthSq() == 0) continue;
                // The angle between them, from 0 to pi
                double angle = Math.atan2(sides[i].cross(sides[j]).length(), sides[i].dot(sides[j]));
                most = Math.max(most, angle);
            }
        }
        return most;
    }
}
