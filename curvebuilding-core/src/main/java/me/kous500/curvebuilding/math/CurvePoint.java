package me.kous500.curvebuilding.math;

import org.jspecify.annotations.Nullable;

/**
 * A point of the curve and its two handles, each of them null until it is set. Along the curve, {@code f} is on the
 * side of the previous point and {@code b} on the side of the next one.
 *
 * @param point the point, or null
 * @param f the handle on the side of the previous point, or null
 * @param b the handle on the side of the next point, or null
 */
public record CurvePoint(@Nullable Vector3 point, @Nullable Vector3 f, @Nullable Vector3 b) {
    public static final CurvePoint EMPTY = new CurvePoint(null, null, null);

    /** The point or one of its handles, as {@code 2}, {@code 2f} and {@code 2b} name them in {@code //bcpos}. */
    public enum Part {
        POINT, F, B
    }

    /**
     * One part of this point.
     *
     * @param part the part
     * @return its position, or null when it is not set
     */
    public @Nullable Vector3 get(Part part) {
        return switch (part) {
            case POINT -> point;
            case F -> f;
            case B -> b;
        };
    }

    /**
     * This point with one part set.
     *
     * @param part the part to set
     * @param position its position, or null to clear it
     * @return the new point
     */
    public CurvePoint with(Part part, @Nullable Vector3 position) {
        return switch (part) {
            case POINT -> new CurvePoint(position, f, b);
            case F -> new CurvePoint(point, position, b);
            case B -> new CurvePoint(point, f, position);
        };
    }

    /**
     * This point with each part that is set moved by {@code offset}.
     *
     * @param offset how far to move
     * @return the moved point
     */
    public CurvePoint moved(Vector3 offset) {
        return new CurvePoint(add(point, offset), add(f, offset), add(b, offset));
    }

    private static @Nullable Vector3 add(@Nullable Vector3 position, Vector3 offset) {
        return position != null ? position.add(offset) : null;
    }
}
