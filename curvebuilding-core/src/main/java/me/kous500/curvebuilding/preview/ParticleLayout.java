package me.kous500.curvebuilding.preview;

import me.kous500.curvebuilding.math.CurvePoint;
import me.kous500.curvebuilding.math.Vector3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

/**
 * The particles that show the curve of a player: the edges of the block of each point, a cross on each handle, a flame
 * at their centers, and dotted lines from the points to their handles and along the curve. The dots of the lines are
 * split into {@link #TURNS} turns, which are sent one after the other, and are further apart the farther they are from
 * the viewer.
 */
public final class ParticleLayout {
    /** How many turns the dots of the lines are split into. */
    public static final int TURNS = 3;

    /** What a particle shows, which decides how it is drawn. */
    public enum Kind {
        /** The edges of the block of the first point. */
        START,
        /** The edges of the block of the last point. */
        END,
        /** The edges of the block of a point between the first and the last. */
        POINT,
        /** The cross on handle f. */
        F,
        /** The cross on handle b. */
        B,
        /** The center of a point or a handle. */
        CENTER,
        /** A dot of the line from a point to one of its handles. */
        HANDLE_LINE,
        /** A dot of the curve. */
        CURVE
    }

    /**
     * One particle.
     *
     * @param position where it is
     * @param kind what it shows
     */
    public record Particle(Vector3 position, Kind kind) {
    }

    /**
     * How densely the particles are laid.
     *
     * @param pointDensity the particles per block on the edges of a block and on a cross, at least 1
     * @param lineDensity the particles per block on the lines, at least 1
     * @param maxLength the longest line, and the longest part of the curve, that is shown, in blocks
     */
    public record Settings(int pointDensity, int lineDensity, int maxLength) {
    }

    private final Vector3 viewer;
    private final int turn;
    private final Settings settings;
    private final List<Particle> particles = new ArrayList<>();

    private ParticleLayout(Vector3 viewer, int turn, Settings settings) {
        this.viewer = viewer;
        this.turn = turn;
        this.settings = settings;
    }

    /**
     * The particles that show {@code points} to a viewer.
     *
     * @param points the points of the curve by their number, at least one
     * @param viewer where the viewer is
     * @param turn which turn of the dots of the lines to include, from 0 to {@link #TURNS} - 1
     * @param settings how densely the particles are laid
     * @return the particles, the points and handles first, then the curve
     */
    public static List<Particle> layout(
            NavigableMap<Integer, CurvePoint> points,
            Vector3 viewer,
            int turn,
            Settings settings) {
        ParticleLayout layout = new ParticleLayout(viewer, turn, settings);
        layout.addPoints(points);
        layout.addCurve(points);
        return layout.particles;
    }

    private void addPoints(NavigableMap<Integer, CurvePoint> points) {
        int last = points.lastKey();
        for (int n = 1; n <= last; n++) {
            CurvePoint point = points.get(n);
            if (point == null) continue;

            for (CurvePoint.Part part : CurvePoint.Part.values()) {
                Vector3 position = point.get(part);
                if (position == null) continue;

                switch (part) {
                    case POINT -> addEdges(position, n == 1 ? Kind.START : n == last ? Kind.END : Kind.POINT);
                    case F -> addCross(position, Kind.F);
                    case B -> addCross(position, Kind.B);
                }
                particles.add(new Particle(position.add(0.5, 0.5, 0.5), Kind.CENTER));
            }

            addHandleLine(point.point(), point.f());
            addHandleLine(point.point(), point.b());
        }
    }

    private void addEdges(Vector3 block, Kind kind) {
        int density = settings.pointDensity();
        double step = 1.0 / density;
        for (int x = 0; x <= density; x++) {
            for (int y = 0; y <= density; y++) {
                for (int z = 0; z <= density; z++) {
                    boolean isXEdge = x == 0 || x == density;
                    boolean isYEdge = y == 0 || y == density;
                    boolean isZEdge = z == 0 || z == density;
                    if ((isXEdge && isYEdge) || (isYEdge && isZEdge) || (isZEdge && isXEdge)) {
                        particles.add(new Particle(block.add(x * step, y * step, z * step), kind));
                    }
                }
            }
        }
    }

    private void addCross(Vector3 block, Kind kind) {
        // Each line of the cross goes from a corner of the block to its center, sqrt(3) / 2 blocks, and has at least
        // its two ends
        int density = Math.max(1, (int) (settings.pointDensity() * Math.sqrt(3) / 2));

        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    for (int i = 0; i <= density; i++) {
                        double ax = 0.5 * x + (double) (density * x + (-x * 2 + 1) * i) / density * 0.5;
                        double ay = 0.5 * y + (double) (density * y + (-y * 2 + 1) * i) / density * 0.5;
                        double az = 0.5 * z + (double) (density * z + (-z * 2 + 1) * i) / density * 0.5;
                        particles.add(new Particle(block.add(ax, ay, az), kind));
                    }
                }
            }
        }
    }

    private void addHandleLine(@Nullable Vector3 point, @Nullable Vector3 handle) {
        if (point == null || handle == null) return;

        int distance = (int) point.distance(handle);
        if (distance > settings.maxLength()) return;

        List<Vector3> candidates = new ArrayList<>();
        for (double t = 0; t <= 1; t += 1.0 / (distance * settings.lineDensity())) {
            candidates.add(Vector3.at(
                    (1 - t) * point.x() + t * handle.x() + 0.5,
                    (1 - t) * point.y() + t * handle.y() + 0.5,
                    (1 - t) * point.z() + t * handle.z() + 0.5));
        }
        addDots(candidates, point, Kind.HANDLE_LINE);
    }

    private void addCurve(NavigableMap<Integer, CurvePoint> points) {
        for (PreviewCurve.ShownSegment shown : PreviewCurve.segments(points, settings.maxLength(), 20)) {
            int divisions = Math.max(1, (int) Math.ceil(shown.lengthToEnd() * settings.lineDensity()));
            addDots(shown.segment().blockCenters(divisions), shown.segment().blockCenter(0), Kind.CURVE);
        }
    }

    /**
     * Adds the dots of a line from {@code candidates}, in order: a candidate becomes a dot when it is far enough from
     * the last dot, and only the dots of this turn are added.
     */
    private void addDots(List<Vector3> candidates, Vector3 start, Kind kind) {
        int count = 0;
        Vector3 last = start;
        for (Vector3 candidate : candidates) {
            double spacing = (viewer.distance(candidate) / 3000 * settings.lineDensity())
                    + (0.8 / settings.lineDensity());
            if (spacing < candidate.distance(last)) {
                if (count % TURNS == turn) particles.add(new Particle(candidate, kind));
                last = candidate;
                count++;
            }
        }
    }
}
