package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.MaxChangedBlocksException;
import me.kous500.curvebuilding.commands.bc.CurvePieces.Vertex;
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
 * <p>Each column of the cross section follows the curve along {@linkplain CurvePieces straight pieces}, close to where
 * the column would go, and lays a block in each block it passes through for at least {@link #LEAST_PASS}: the block of
 * the selection at the distance along the curve where the column enters it.
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

    /** The least length, in blocks, that a column goes through a block to lay one there. */
    private static final double LEAST_PASS = 0.1;

    /** The curve, or a segment of it, is longer than the {@code max-set-length} of the config. */
    static final class TooLongException extends Exception {
        TooLongException() {
            super(null, null, false, false);
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
    private final CurvePieces pieces;
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
        this.pieces = new CurvePieces(outermost);
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

        List<Vertex> along = pieces.along(segment);
        double segmentLength = along.getLast().distance();
        // Two positions at the same place with no handles between them: nothing to place
        if (segmentLength == 0) return;
        if (addedLength + segmentLength > maxLength) throw new TooLongException();

        List<Vertex> vertices = new ArrayList<>();
        boolean steep = isSteep(along);
        boolean splitsWithBefore = false;
        if (lastAcross != null) {
            double turn = CurvePieces.turn(lastAcross, along.getFirst().across());
            vertices.addAll(pieces.corner(segment.p0(), lastAcross, turn));
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
         * {@code fromDistance} and {@code toDistance} along the curve, through each block it crosses, in the order of
         * a {@link BlockWalk}. A column that skipped a piece inside a corner jumps to {@code from}.
         */
        void follow(int index, Vector3 from, Vector3 to, double fromDistance, double toDistance)
                throws MaxChangedBlocksException {
            BlockWalk walk = new BlockWalk(from, to);
            if (!walk.block().equals(block)) enter(walk.block(), index, fromDistance);

            double length = from.distance(to);
            if (length == 0) return;

            double fraction = 0;
            while (walk.next()) {
                passed += (walk.entered() - fraction) * length;
                fraction = walk.entered();
                // Where the curve itself, not moved by EDGE, crosses the edge
                double edgeOfTheCurve = Math.clamp(walk.enteredWhenMovedBack(EDGE), 0, 1);
                enter(walk.block(), index, fromDistance + (toDistance - fromDistance) * edgeOfTheCurve);
            }
            passed += (1 - fraction) * length;
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
