package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.MaxChangedBlocksException;
import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Where {@code //bc} places which block of the selection along a curve. The selection is read along one axis: its
 * cross section across that axis is laid along the curve, turned with it and centered on it, with its bottom row at the
 * curve, and the selection repeats along the curve, starting {@code offset} blocks into it. Where the curve turns at a
 * point, the cross section turns around the point, filling the outside of the corner.
 *
 * <p>Each column of the cross section follows the curve along straight pieces, which stray at most {@link #TOLERANCE}
 * from where the column would go, and lays a block in each block it passes through for at least {@link #LEAST_PASS}:
 * the block of the selection at the distance along the curve where the column enters it.
 *
 * <p>The cross section is laid row by row from the middle outwards, and in each row column by column from the middle
 * outwards, each column along the whole curve before the next. A block that several blocks of the selection fall on
 * keeps the first. Where the curve climbs or falls within an xz cell (a cell of the xz plane: the blocks at its x and
 * z, at any height), each row passes more than one block there, and the rows laid first keep the blocks they share:
 * the blocks left over go to the rows nearest the middle, so that with three rows or more, the top and bottom rows
 * keep one each.
 *
 * <p>Where two segments meet at a corner, turning by more than {@link #CORNER} on the xz plane, and neither is steep,
 * neither places anything in an xz cell, at any height, where the other has laid first: inside the corner, the two
 * cross sections meet where they are equally far from the middle, instead of crossing each other where the curve climbs
 * or falls. A steep segment, which climbs or falls more than it goes across the xz plane, passes few xz cells, most of
 * which the segment beside it would take, so it splits none.
 *
 * <p>Add the segments of the curve in order with {@link #add}, then take the blocks with {@link #blocks}, which lays
 * them. The world is only read through {@code isAir}, for {@code -a}, and nothing is placed.
 */
final class CurveLayout {
    /**
     * The selection, as WorldEdit gives it.
     *
     * @param width the size along x
     * @param height the size along y
     * @param length the size along z
     * @param min the corner with the lowest coordinates
     */
    record Selection(int width, int height, int length, BlockVector3 min) {
    }

    /**
     * Added to a position before taking its block, so that a position on the edge between two blocks goes to the one
     * with the higher coordinate, also when rounding puts it just below the edge.
     */
    private static final double EDGE = 1e-6;

    /**
     * The least turn, in radians on the xz plane, where two segments meet at a corner. Handles that {@code //bcpos}
     * keeps in line turn by about none, and a point without handles where the curve turns by much more.
     */
    private static final double CORNER = Math.toRadians(1);

    /**
     * How far, in blocks, the straight pieces that the columns follow may stray from the curve: from the middle of the
     * cross section and from its outermost columns.
     */
    private static final double TOLERANCE = 0.05;

    /** The least length, in blocks, that a column goes through a block to lay one there. */
    private static final double LEAST_PASS = 0.1;

    /** The most times that a part of a segment is split in half to follow it with straight pieces. */
    private static final int MAX_SPLITS = 20;

    /** The points of the five-point Gauss-Legendre rule on -1 to 1, with which the length of a piece is measured. */
    private static final double[] GAUSS_POINTS = {
            0, -0.5384693101056831, 0.5384693101056831, -0.9061798459386640, 0.9061798459386640
    };

    /** The weights of {@link #GAUSS_POINTS}, in the same order. */
    private static final double[] GAUSS_WEIGHTS = {
            0.5688888888888889, 0.4786286704993665, 0.4786286704993665, 0.2369268850561891, 0.2369268850561891
    };

    /** The curve, or a segment of it, is longer than the {@code max-set-length} of the config. */
    static final class TooLongException extends Exception {
        TooLongException() {
            super(null, null, false, false);
        }
    }

    /**
     * A vertex of the straight pieces that the columns follow along a segment.
     *
     * @param point where the curve is
     * @param across the way to the left of the curve, on the xz plane and of length 1
     * @param distance how far along the curve the vertex is from the start of the segment
     * @param insideSide for a vertex that turns the cross section at a corner, the side of the curve inside the corner:
     * 1 on the left, -1 on the right; 0 for a vertex along the segment
     */
    private record Vertex(Vector3 point, Vector3 across, double distance, int insideSide) {
        /**
         * Whether a column {@code side} blocks to the left of the curve is inside the corner that this vertex turns
         * at, where the column skips the piece to the next vertex.
         */
        boolean isInsideCorner(double side) {
            return insideSide * side > 0;
        }
    }

    /**
     * A segment added, to lay with the others.
     *
     * @param vertices its vertices, after those that turn the cross section at its start
     * @param start how far along the curve it starts
     * @param splitsWithBefore whether it splits the xz cells it shares with the segment before: whether they meet at a
     * corner and neither is steep
     */
    private record LaidSegment(List<Vertex> vertices, double start, boolean splitsWithBefore) {
    }

    /**
     * A cell of the xz plane: the blocks at its x and z, at any height.
     *
     * @param x the x of its blocks
     * @param z the z of its blocks
     */
    private record XzCell(int x, int z) {
        /** The xz cell that {@code block} is in. */
        static XzCell of(BlockVector3 block) {
            return new XzCell(block.x(), block.z());
        }
    }

    private final Selection selection;
    private final BcOptions.Axis axis;
    /** The number of columns across the selection, the width of the cross section. */
    private final int columns;
    /** How far the outermost columns of the cross section are from its middle, which is that far from column 0. */
    private final double outermost;
    /** The size of the selection along the axis, after which it repeats along the curve. */
    private final int repeat;
    private final int offset;
    private final boolean airOnly;
    private final Predicate<BlockVector3> isAir;
    private final int maxBlocks;
    private final int maxLength;

    /** The segments added so far, in order. */
    private final List<LaidSegment> segments = new ArrayList<>();
    /** The length of the segments added so far, where the next one goes on from. */
    private double addedLength;
    /** The way to the left of the curve at the end of the segments added so far, or null before the first. */
    private @Nullable Vector3 lastAcross;
    /** Whether the last segment added is steep. */
    private boolean lastSteep;

    /** Whether {@link #blocks} has laid the segments. */
    private boolean laid;
    /** For each block to place, the block of the selection laid there first. */
    private final Map<BlockVector3, BlockVector3> sources = new HashMap<>();
    /**
     * For each xz cell, the segments, by their index, that split xz cells with the segment before or after and have
     * taken it.
     */
    private final Map<XzCell, Set<Integer>> xzCellsTaken = new HashMap<>();
    /** The blocks found not to be air, for {@code -a}, so that the world is read once for each block. */
    private final Set<BlockVector3> notAir = new HashSet<>();

    /**
     * A layout with nothing laid yet. The selection is read along the axis of {@code arguments}, or without one along
     * the shorter of x and z, x when they are the same.
     *
     * @param selection the selection to lay along the curve
     * @param arguments the arguments of {@code //bc}
     * @param isAir whether the block at a position in the world is air, for {@code -a}
     * @param maxBlocks the most blocks to place, or 0 or less for no limit
     * @param maxLength the longest curve, from the {@code max-set-length} of the config
     */
    CurveLayout(
            Selection selection,
            BcArguments arguments,
            Predicate<BlockVector3> isAir,
            int maxBlocks,
            int maxLength) {
        this.selection = selection;
        if (arguments.axis() != null) {
            this.axis = arguments.axis();
        } else {
            this.axis = selection.width() <= selection.length() ? BcOptions.Axis.X : BcOptions.Axis.Z;
        }
        this.columns = axis == BcOptions.Axis.X ? selection.length() : selection.width();
        this.outermost = (columns - 1) / 2.0;
        this.repeat = axis == BcOptions.Axis.X ? selection.width() : selection.length();
        this.offset = arguments.offset();
        this.airOnly = arguments.air();
        this.isAir = isAir;
        this.maxBlocks = maxBlocks;
        this.maxLength = maxLength;
    }

    /**
     * Adds {@code segment} to the curve, going on from the segments added before. Nothing is laid until
     * {@link #blocks}. After an exception, the layout is not to be used.
     *
     * @param segment the next segment of the curve
     * @throws TooLongException when the ends of the segment, or an end and its handle, are further apart than
     * {@code maxLength}, or the curve with the segment is longer than it
     */
    void add(BezierSegment segment) throws TooLongException {
        if (!segment.isWithin(maxLength)) throw new TooLongException();

        List<Vertex> along = vertices(segment);
        double segmentLength = along.getLast().distance();
        // Two positions at the same place with no handles between them: nothing to place
        if (segmentLength == 0) return;
        if (addedLength + segmentLength > maxLength) throw new TooLongException();

        List<Vertex> vertices = new ArrayList<>();
        boolean steep = isSteep(along);
        boolean splitsWithBefore = false;
        if (lastAcross != null) {
            double turn = turn(lastAcross, along.getFirst().across());
            vertices.addAll(corner(segment.p0(), lastAcross, turn));
            splitsWithBefore = Math.abs(turn) > CORNER && !lastSteep && !steep;
        }
        vertices.addAll(along);

        segments.add(new LaidSegment(vertices, addedLength, splitsWithBefore));
        addedLength += segmentLength;
        lastAcross = along.getLast().across();
        lastSteep = steep;
    }

    /**
     * The blocks to place and, for each, the block of the selection to place there. The first call lays the segments
     * added; after an exception, the layout is not to be used.
     *
     * @return a copy, in no particular order
     * @throws MaxChangedBlocksException when the curve would place more than {@code maxBlocks} blocks
     */
    Map<BlockVector3, BlockVector3> blocks() throws MaxChangedBlocksException {
        if (!laid) {
            laid = true;
            layAll();
        }
        return new HashMap<>(sources);
    }

    /**
     * The length of the curve added so far, measured along the curve itself, not along the straight pieces.
     *
     * @return the length in blocks
     */
    double length() {
        return addedLength;
    }

    /** Lays the cross section along the curve, in the order that the class describes. */
    private void layAll() throws MaxChangedBlocksException {
        int[] columnsFromTheMiddle = fromTheMiddle(columns);
        for (int row : fromTheMiddle(selection.height())) {
            for (int column : columnsFromTheMiddle) {
                layColumn(row, column);
            }
        }
    }

    /**
     * The vertices of straight pieces along {@code segment}, from its start to its end. A part of the segment is split
     * in half until its control polygon is within {@link #TOLERANCE} of the line between its ends, and the outermost
     * columns turn with it by little enough to stray at most that far, or it has been split {@link #MAX_SPLITS} times.
     */
    private List<Vertex> vertices(BezierSegment segment) {
        List<Vertex> vertices = new ArrayList<>();
        vertices.add(new Vertex(segment.p0(), across(segment.tangent(0)), 0, 0));
        addVertices(segment, segment, 0, 1, 0, vertices);
        return vertices;
    }

    /**
     * Adds to {@code vertices} those of the part of {@code segment} from t = {@code from} to {@code to}, whose control
     * points are those of {@code part}, after the vertex at {@code from}, which {@code vertices} ends with.
     */
    private void addVertices(BezierSegment segment, BezierSegment part, double from, double to, int splits,
            List<Vertex> vertices) {
        if (splits < MAX_SPLITS && !isStraight(part)) {
            double middle = (from + to) / 2;
            BezierSegment[] halves = halves(part);
            addVertices(segment, halves[0], from, middle, splits + 1, vertices);
            addVertices(segment, halves[1], middle, to, splits + 1, vertices);
            return;
        }
        double distance = vertices.getLast().distance() + length(segment, from, to);
        vertices.add(new Vertex(segment.point(to), across(segment.tangent(to)), distance, 0));
    }

    /**
     * Whether the columns may follow {@code part} of a segment along one straight piece: its control polygon, which
     * the curve stays within, is within {@link #TOLERANCE} of the line between its ends, and the outermost columns,
     * which turn with the curve around its middle, stray at most that far from the straight piece between their ends.
     */
    private boolean isStraight(BezierSegment part) {
        if (distanceToLine(part.p1(), part.p0(), part.p3()) > TOLERANCE) return false;
        if (distanceToLine(part.p2(), part.p0(), part.p3()) > TOLERANCE) return false;
        // A column turned by an angle around a point strays from the straight line between its ends by about
        // outermost * angle^2 / 8
        double turn = largestTurn(part);
        return outermost * turn * turn / 8 <= TOLERANCE;
    }

    /** How far {@code point} is from the straight line from {@code from} to {@code to}, between those ends. */
    private static double distanceToLine(Vector3 point, Vector3 from, Vector3 to) {
        Vector3 line = to.subtract(from);
        Vector3 toPoint = point.subtract(from);
        double lengthSq = line.lengthSq();
        double along = lengthSq == 0
                ? 0
                : (toPoint.x() * line.x() + toPoint.y() * line.y() + toPoint.z() * line.z()) / lengthSq;
        along = Math.clamp(along, 0, 1);
        return point.distance(from.add(line.multiply(along)));
    }

    /**
     * The most, in radians on the xz plane, that the curve along {@code part} turns: at most the angle between the
     * sides of its control polygon, since the way the curve goes is always a sum of those sides, weighted by the
     * quadratic Bernstein polynomials. Sides straight up or down do not turn it.
     */
    private static double largestTurn(BezierSegment part) {
        Vector3[] sides = {part.p1().subtract(part.p0()), part.p2().subtract(part.p1()), part.p3().subtract(part.p2())};
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

    /** The two halves of {@code part}, cut at t = 0.5, by the construction of de Casteljau. */
    private static BezierSegment[] halves(BezierSegment part) {
        Vector3 a = halfway(part.p0(), part.p1());
        Vector3 b = halfway(part.p1(), part.p2());
        Vector3 c = halfway(part.p2(), part.p3());
        Vector3 ab = halfway(a, b);
        Vector3 bc = halfway(b, c);
        Vector3 middle = halfway(ab, bc);
        return new BezierSegment[]{
                new BezierSegment(part.p0(), a, ab, middle),
                new BezierSegment(middle, bc, c, part.p3())
        };
    }

    private static Vector3 halfway(Vector3 from, Vector3 to) {
        return from.add(to).multiply(0.5);
    }

    /**
     * The length of {@code segment} from t = {@code from} to {@code to}, by the five-point Gauss-Legendre rule on its
     * speed, the length of its derivative.
     */
    private static double length(BezierSegment segment, double from, double to) {
        double half = (to - from) / 2;
        double middle = (from + to) / 2;
        double sum = 0;
        for (int i = 0; i < GAUSS_POINTS.length; i++) {
            // The derivative is 3 times the direction
            sum += GAUSS_WEIGHTS[i] * 3 * segment.direction(middle + half * GAUSS_POINTS[i]).length();
        }
        return half * sum;
    }

    /**
     * The angle on the xz plane from {@code from} to {@code to}, the shorter way round: from -pi to pi, positive when
     * it turns from +x towards +z.
     */
    private static double turn(Vector3 from, Vector3 to) {
        return Math.atan2(from.x() * to.z() - from.z() * to.x(), from.x() * to.x() + from.z() * to.z());
    }

    /** Whether the curve along {@code vertices} climbs or falls more than it goes across the xz plane. */
    private static boolean isSteep(List<Vertex> vertices) {
        double across = 0;
        double upAndDown = 0;
        for (int i = 1; i < vertices.size(); i++) {
            Vector3 from = vertices.get(i - 1).point();
            Vector3 to = vertices.get(i).point();
            across += Math.hypot(to.x() - from.x(), to.z() - from.z());
            upAndDown += Math.abs(to.y() - from.y());
        }
        return upAndDown > across;
    }

    /**
     * Vertices at {@code point}, where a segment starts, that turn the cross section from {@code from}, the way across
     * the end of the segment before, by {@code angle}, towards the way across the start of this one, which the first
     * vertex of the segment has. The outermost columns go around the outside of the corner along straight pieces that
     * stray at most {@link #TOLERANCE} from the arc; none when the angle is 0.
     */
    private List<Vertex> corner(Vector3 point, Vector3 from, double angle) {
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
     * The rows or the columns of a cross section in the order to lay them: from the middle outwards, so that where two
     * fall on the same block, the one nearer the middle keeps it. Of two as far from the middle, the lower comes first:
     * 5 give 2, 1, 3, 0, 4, and 4 give 1, 2, 0, 3.
     *
     * @param count the number of rows or columns
     * @return each from 0 to {@code count - 1} once
     */
    private static int[] fromTheMiddle(int count) {
        double middle = (count - 1) / 2.0;
        return IntStream.range(0, count)
                .boxed()
                .sorted(Comparator.comparingDouble(index -> Math.abs(index - middle)))
                .mapToInt(Integer::intValue)
                .toArray();
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
     * How far to the left of the curve column {@code column} of the selection is laid. The selection is turned, not
     * mirrored: read along x, its +z is on the right of the curve; read along z, its +x is on the left.
     */
    private double side(int column) {
        return axis == BcOptions.Axis.X ? outermost - column : column - outermost;
    }

    /**
     * Lays one column of the cross section along the whole curve: the blocks of the selection along the axis, in row
     * {@code row} from the bottom and column {@code column} from the lowest coordinate across it. The column follows
     * the pieces between the vertices of each segment in order, skipping those inside a corner.
     */
    private void layColumn(int row, int column) throws MaxChangedBlocksException {
        double side = side(column);
        ColumnPass pass = new ColumnPass(row, column);
        for (int index = 0; index < segments.size(); index++) {
            LaidSegment segment = segments.get(index);
            List<Vertex> vertices = segment.vertices();
            for (int i = 0; i + 1 < vertices.size(); i++) {
                Vertex from = vertices.get(i);
                if (from.isInsideCorner(side)) continue;
                Vertex to = vertices.get(i + 1);
                pass.follow(
                        index,
                        positionOf(from, side, row),
                        positionOf(to, side, row),
                        segment.start() + from.distance(),
                        segment.start() + to.distance()
                );
            }
        }
        pass.finish();
    }

    /**
     * Where the column {@code side} blocks to the left of the curve is at {@code vertex} in {@code row}, moved by
     * {@link #EDGE} so that its block is the one it is in with the higher coordinate on the edge between two.
     */
    private static Vector3 positionOf(Vertex vertex, double side, int row) {
        return vertex.point().add(vertex.across().multiply(side)).add(EDGE, row + EDGE, EDGE);
    }

    /**
     * A column followed along the curve, block by block, laying each block it leaves when it went through for at
     * least {@link #LEAST_PASS}.
     */
    private final class ColumnPass {
        private final int row;
        private final int column;
        /** The block the column is in, or null before it starts. */
        private @Nullable BlockVector3 block;
        /** The segment, by its index, along which the column entered {@link #block}. */
        private int entrySegment;
        /** How far along the curve the column entered {@link #block}. */
        private double entryDistance;
        /** How far the column has gone through {@link #block}. */
        private double passed;

        ColumnPass(int row, int column) {
            this.row = row;
            this.column = column;
        }

        /**
         * Follows the straight piece from {@code from} to {@code to} along segment {@code index}, which are
         * {@code fromDistance} and {@code toDistance} along the curve, through each block it crosses, as Amanatides
         * and Woo traverse a grid. A column that skipped a piece inside a corner jumps to {@code from}.
         */
        void follow(int index, Vector3 from, Vector3 to, double fromDistance, double toDistance)
                throws MaxChangedBlocksException {
            BlockVector3 start = BlockVector3.floor(from);
            if (!start.equals(block)) enter(start, index, fromDistance);

            Vector3 way = to.subtract(from);
            double length = way.length();
            if (length == 0) return;

            double[] origin = {from.x(), from.y(), from.z()};
            double[] delta = {way.x(), way.y(), way.z()};
            int[] cell = {start.x(), start.y(), start.z()};
            int[] step = new int[3];
            // For each axis, the fraction of the piece at which it crosses the next edge, and between two edges
            double[] nextEdge = new double[3];
            double[] betweenEdges = new double[3];
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

            double fraction = 0;
            while (true) {
                int axis = nextEdge[0] <= nextEdge[1]
                        ? (nextEdge[0] <= nextEdge[2] ? 0 : 2)
                        : (nextEdge[1] <= nextEdge[2] ? 1 : 2);
                double crossing = nextEdge[axis];
                if (crossing >= 1) {
                    passed += (1 - fraction) * length;
                    return;
                }
                passed += (crossing - fraction) * length;
                cell[axis] += step[axis];
                nextEdge[axis] += betweenEdges[axis];
                fraction = crossing;
                // Where the curve itself, not moved by EDGE, crosses the edge
                double edgeOfTheCurve = Math.clamp(crossing + EDGE / delta[axis], 0, 1);
                enter(
                        BlockVector3.at(cell[0], cell[1], cell[2]),
                        index,
                        fromDistance + (toDistance - fromDistance) * edgeOfTheCurve
                );
            }
        }

        /** Lays the last block the column is in, once it has followed the whole curve. */
        void finish() throws MaxChangedBlocksException {
            lay();
        }

        private void enter(BlockVector3 next, int index, double distance) throws MaxChangedBlocksException {
            lay();
            block = next;
            entrySegment = index;
            entryDistance = distance;
            passed = 0;
        }

        /**
         * Lays the block the column is in, unless it went through for less than {@link #LEAST_PASS},
         * {@link #takeXzCell} keeps it from that xz cell, a block was laid there before, or with {@code -a} it is not
         * air.
         */
        private void lay() throws MaxChangedBlocksException {
            if (block == null || passed < LEAST_PASS) return;
            if (!takeXzCell(entrySegment, block)) return;
            if (sources.containsKey(block) || !mayPlace(block)) return;
            sources.put(block, source(entryDistance, row, column));
            if (0 < maxBlocks && maxBlocks < sources.size()) {
                throw new MaxChangedBlocksException(maxBlocks);
            }
        }
    }

    /**
     * Whether segment {@code index} may lay in the xz cell that {@code target} is in, taking it when it splits xz
     * cells with the segment before or after: false when such a segment has taken the xz cell first.
     */
    private boolean takeXzCell(int index, BlockVector3 target) {
        boolean splitsWithBefore = segments.get(index).splitsWithBefore();
        boolean splitsWithAfter = index + 1 < segments.size() && segments.get(index + 1).splitsWithBefore();
        if (!splitsWithBefore && !splitsWithAfter) return true;

        Set<Integer> taken = xzCellsTaken.computeIfAbsent(XzCell.of(target), key -> new HashSet<>());
        if (splitsWithBefore && taken.contains(index - 1) || splitsWithAfter && taken.contains(index + 1)) return false;
        taken.add(index);
        return true;
    }

    /** The block of the selection in {@code row} and {@code column} for {@code distance} along the curve. */
    private BlockVector3 source(double distance, int row, int column) {
        // How far into the repeating selection the block is; on the edge between two, the further one
        int along = Math.floorMod((int) Math.floor(distance + offset + 0.5 + EDGE), repeat);
        return axis == BcOptions.Axis.X
                ? selection.min().add(along, row, column)
                : selection.min().add(column, row, along);
    }

    /**
     * Whether a block of the selection may go to {@code target}, which has none yet: always without {@code -a}, and
     * with it when the block there is air.
     */
    private boolean mayPlace(BlockVector3 target) {
        if (!airOnly) return true;
        if (notAir.contains(target)) return false;
        if (isAir.test(target)) return true;

        notAir.add(target);
        return false;
    }
}
