package me.kous500.curvebuilding.curve;

import me.kous500.curvebuilding.math.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

/**
 * A cubic Bézier curve from {@code p0} to {@code p3}, pulled towards {@code p1} and {@code p2}: the part of a curve
 * between two of its points. Each position is the corner of a block, so the curve goes through the corners; {@link
 * #blockCenter} gives the centers that the previews draw.
 *
 * @param p0 where the segment starts
 * @param p1 the handle on the side of {@code p0}
 * @param p2 the handle on the side of {@code p3}
 * @param p3 where the segment ends
 */
public record BezierSegment(Vector3 p0, Vector3 p1, Vector3 p2, Vector3 p3) {
    public BezierSegment {
        Objects.requireNonNull(p0, "p0");
        Objects.requireNonNull(p1, "p1");
        Objects.requireNonNull(p2, "p2");
        Objects.requireNonNull(p3, "p3");
    }

    /**
     * The segments through points 1, 2, ..., one for each two points in a row. The segment from point n - 1 to n goes
     * through handle b of n - 1 and handle f of n; a handle that is not set is at its point. Stops at the first point
     * that is not set.
     *
     * @param points the points by their number
     * @return the segments in order, empty with fewer than two points
     */
    public static List<BezierSegment> through(NavigableMap<Integer, CurvePoint> points) {
        List<BezierSegment> segments = new ArrayList<>();
        CurvePoint first = points.get(1);
        if (first == null || first.point() == null) return segments;

        Vector3 before = first.point();
        Vector3 beforeHandle = requireNonNullElse(first.b(), before);
        for (int n = 2; n <= points.lastKey(); n++) {
            CurvePoint p = points.get(n);
            if (p == null || p.point() == null) break;

            Vector3 point = p.point();
            segments.add(new BezierSegment(before, beforeHandle, requireNonNullElse(p.f(), point), point));
            before = point;
            beforeHandle = requireNonNullElse(p.b(), point);
        }
        return segments;
    }

    /**
     * The point at {@code t}, from 0 at {@code p0} to 1 at {@code p3}.
     *
     * @param t where on the segment, from 0 to 1
     * @return the point
     * @throws IllegalArgumentException when {@code t} is outside 0 to 1
     */
    public Vector3 point(double t) {
        if (t < 0 || t > 1) {
            throw new IllegalArgumentException("t must be in the range 0 to 1");
        }

        // The cubic Bernstein polynomials: how much each point pulls the curve at t
        double u = 1 - t;
        double cubic0 = u * u * u;
        double cubic1 = 3 * u * u * t;
        double cubic2 = 3 * u * t * t;
        double cubic3 = t * t * t;

        return p0.multiply(cubic0)
                .add(p1.multiply(cubic1))
                .add(p2.multiply(cubic2))
                .add(p3.multiply(cubic3));
    }

    /**
     * The center of the block whose corner is {@link #point} at {@code t}.
     *
     * @param t where on the segment, from 0 to 1
     * @return the center of the block
     */
    public Vector3 blockCenter(double t) {
        return point(t).add(0.5, 0.5, 0.5);
    }

    /**
     * The direction of the curve at {@code t} on the xz plane, as the angle in radians from +z towards +x, from
     * {@code -Math.PI} to {@code Math.PI}. 0 when the curve does not move on the xz plane there.
     *
     * @param t where on the segment, from 0 to 1
     * @return the angle in radians
     */
    public double angle(double t) {
        // The direction of the curve, its derivative divided by 3: the steps between the points, weighted by the
        // quadratic Bernstein polynomials
        double u = 1 - t;
        double quadratic0 = u * u;
        double quadratic1 = 2 * u * t;
        double quadratic2 = t * t;

        Vector3 step0 = p1.subtract(p0);
        Vector3 step1 = p2.subtract(p1);
        Vector3 step2 = p3.subtract(p2);

        Vector3 direction = step0.multiply(quadratic0)
                .add(step1.multiply(quadratic1))
                .add(step2.multiply(quadratic2));

        // atan2 gives Math.PI or -Math.PI for a zero vector when z is -0.0
        if (direction.x() == 0 && direction.z() == 0) return 0;
        return Math.atan2(direction.x(), direction.z());
    }

    /**
     * The length of the curve, measured along {@code fineness} straight steps.
     *
     * @param fineness the number of steps, greater than 0; the steps stop short of the end when it is not a whole
     * number
     * @return the length in blocks
     * @throws IllegalArgumentException when {@code fineness} is 0 or less
     */
    public double length(double fineness) {
        if (fineness <= 0) {
            throw new IllegalArgumentException("fineness must be greater than 0");
        }

        double length = 0;
        Vector3 before = blockCenter(0);
        for (int step = 1; step <= fineness; step++) {
            Vector3 p = blockCenter(step / fineness);
            length += Math.sqrt((p.x() - before.x()) * (p.x() - before.x())
                    + (p.y() - before.y()) * (p.y() - before.y())
                    + (p.z() - before.z()) * (p.z() - before.z()));
            before = p;
        }
        return length;
    }

    /**
     * A fineness for {@link #length}: {@code stepsPerBlock} steps per block of the control polygon, at least 1. The
     * polygon, not the distance between the ends, so that a curve whose ends are at the same place still gets steps
     * along its handles.
     *
     * @param stepsPerBlock the steps per block
     * @return the fineness
     */
    public double fineness(double stepsPerBlock) {
        double polygonLength = p0.distance(p1) + p1.distance(p2) + p2.distance(p3);
        return Math.max(1, polygonLength * stepsPerBlock);
    }

    /**
     * Whether the ends, and each end and its handle, are at most {@code maxLength} apart.
     *
     * @param maxLength the longest distance allowed, in blocks
     * @return whether every distance is within it
     */
    public boolean isWithin(double maxLength) {
        return p0.distance(p3) <= maxLength && p0.distance(p1) <= maxLength && p3.distance(p2) <= maxLength;
    }

    /**
     * {@link #blockCenter Block centers} at evenly spaced positions from 0 to 1, both ends included.
     *
     * @param divisions the number of parts to split the curve into, at least 1
     * @return the {@code divisions + 1} block centers, from {@code p0} to {@code p3}
     */
    public List<Vector3> blockCenters(int divisions) {
        if (divisions < 1) {
            throw new IllegalArgumentException("divisions must be at least 1");
        }

        List<Vector3> points = new ArrayList<>(divisions + 1);
        for (int step = 0; step <= divisions; step++) {
            points.add(blockCenter((double) step / divisions));
        }
        return points;
    }
}
