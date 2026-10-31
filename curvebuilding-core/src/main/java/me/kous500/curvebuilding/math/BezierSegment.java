package me.kous500.curvebuilding.math;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;

/**
 * A cubic Bézier curve from {@code p0} to {@code p3}, pulled towards {@code p1} and {@code p2}: the part of a curve
 * between two of its points. Each position is the corner of a block, so the curve goes through the corners;
 * {@link #blockCenter} gives the centers that the previews draw.
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
     * @return the segments in order, empty with fewer than two points or with null
     */
    public static List<BezierSegment> through(NavigableMap<Integer, CurvePoint> points) {
        List<BezierSegment> segments = new ArrayList<>();
        if (points == null || points.isEmpty()) return segments;

        for (int n = 1; n <= points.lastKey(); n++) {
            CurvePoint p = points.get(n);
            if (p == null || p.point() == null) break;

            if (n > 1) {
                CurvePoint before = points.get(n - 1);
                segments.add(new BezierSegment(
                        before.point(),
                        before.b() != null ? before.b() : before.point(),
                        p.f() != null ? p.f() : p.point(),
                        p.point()
                ));
            }
        }
        return segments;
    }

    /** The point at {@code t}, from 0 at {@code p0} to 1 at {@code p3}. */
    public Vector3 point(double t) {
        if (t < 0 || t > 1) {
            throw new IllegalArgumentException("t must be in the range 0 to 1");
        }

        double u = 1 - t;
        double bernstein0 = u * u * u;
        double bernstein1 = 3 * u * u * t;
        double bernstein2 = 3 * u * t * t;
        double bernstein3 = t * t * t;

        return Vector3.at(
                bernstein0 * p0.x() + bernstein1 * p1.x() + bernstein2 * p2.x() + bernstein3 * p3.x(),
                bernstein0 * p0.y() + bernstein1 * p1.y() + bernstein2 * p2.y() + bernstein3 * p3.y(),
                bernstein0 * p0.z() + bernstein1 * p1.z() + bernstein2 * p2.z() + bernstein3 * p3.z()
        );
    }

    /** The center of the block whose corner is {@link #point} at {@code t}. */
    public Vector3 blockCenter(double t) {
        return point(t).add(0.5, 0.5, 0.5);
    }

    /**
     * The direction of the curve at {@code t} on the xz plane, as the angle in radians from +z towards +x. 0 when the
     * curve does not move on the xz plane there.
     */
    public double angle(double t) {
        double dx = 3 * t * t * (-p0.x() + 3 * p1.x() - 3 * p2.x() + p3.x()) + 6 * t * (p0.x() - 2 * p1.x() + p2.x())
                + 3 * (-p0.x() + p1.x());
        double dz = 3 * t * t * (-p0.z() + 3 * p1.z() - 3 * p2.z() + p3.z()) + 6 * t * (p0.z() - 2 * p1.z() + p2.z())
                + 3 * (-p0.z() + p1.z());

        double angle = Math.atan(dx / dz);
        if (Double.isNaN(angle)) {
            angle = dx > 0 ? Math.PI / 2 : dx < 0 ? -Math.PI / 2 : 0;
        }
        if (dz < 0) angle += Math.PI;
        return angle;
    }

    /** The length of the curve, measured along {@code fineness} straight steps. */
    public double length(double fineness) {
        if (fineness <= 0) {
            throw new IllegalArgumentException("fineness must be greater than 0");
        }

        double length = 0;
        Vector3 before = blockCenter(0);
        for (int step = 1; step <= fineness; step++) {
            Vector3 p = blockCenter(step / fineness);
            length += Math.sqrt((p.x() - before.x()) * (p.x() - before.x()) + (p.y() - before.y()) * (p.y() - before.y())
                    + (p.z() - before.z()) * (p.z() - before.z()));
            before = p;
        }
        return length;
    }

    /**
     * A fineness for {@link #length}: {@code stepsPerBlock} steps per block of the control polygon, at least 1. The
     * polygon, not the distance between the ends, so that a curve whose ends are at the same place still gets steps
     * along its handles.
     */
    public double fineness(double stepsPerBlock) {
        double polygonLength = p0.distance(p1) + p1.distance(p2) + p2.distance(p3);
        return Math.max(1, polygonLength * stepsPerBlock);
    }

    /** Whether the ends, and each end and its handle, are at most {@code maxLength} apart. */
    public boolean isWithin(double maxLength) {
        return p0.distance(p3) <= maxLength && p0.distance(p1) <= maxLength && p3.distance(p2) <= maxLength;
    }

    /**
     * {@link #blockCenter Block centers} at evenly spaced positions from 0 to 1, both ends included.
     *
     * @param divisions the number of parts to split the curve into; the result has {@code divisions + 1} points
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
