package me.kous500.curvebuilding.math;

/**
 * A point of the curve and its two handles, each of them null until it is set. Along the curve, {@code f} is on the
 * side of the previous point and {@code b} on the side of the next one.
 */
public record CurvePoint(Vector3 point, Vector3 f, Vector3 b) {
    public static final CurvePoint EMPTY = new CurvePoint(null, null, null);

    /** The point or one of its handles, as {@code 2}, {@code 2f} and {@code 2b} name them in {@code //bcpos}. */
    public enum Part {
        POINT, F, B
    }

    public Vector3 get(Part part) {
        return switch (part) {
            case POINT -> point;
            case F -> f;
            case B -> b;
        };
    }

    public CurvePoint with(Part part, Vector3 position) {
        return switch (part) {
            case POINT -> new CurvePoint(position, f, b);
            case F -> new CurvePoint(point, position, b);
            case B -> new CurvePoint(point, f, position);
        };
    }

    /** This point with each part that is set moved by {@code offset}. */
    public CurvePoint moved(Vector3 offset) {
        return new CurvePoint(add(point, offset), add(f, offset), add(b, offset));
    }

    private static Vector3 add(Vector3 position, Vector3 offset) {
        return position != null ? position.add(offset) : null;
    }
}
