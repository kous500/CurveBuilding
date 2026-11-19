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
 * <p>The cross section is laid row by row from the bottom, and in each row column by column from the middle outwards,
 * each column along every segment in order before the next. A block that several blocks of the selection fall on keeps
 * the first. Where the curve climbs or falls within an xz cell (a cell of the xz plane: the blocks at its x and z, at
 * any height), a row passes two blocks there; the row below has laid on the lower one first, so each row keeps one. For
 * the bottom row, a row below it is worked out first, laying nothing, to keep the bottom row of the same segment off
 * those blocks.
 *
 * <p>Where two segments meet at a corner, turning by more than {@link #CORNER} on the xz plane, neither places anything
 * in an xz cell, at any height, where the other has laid first: inside the corner, the two cross sections meet where
 * they are equally far from the middle, instead of crossing each other where the curve climbs or falls.
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

    /** The steps per block of the control polygon of a segment to measure it roughly, to choose its number of steps. */
    private static final int ROUGH_STEPS_PER_BLOCK = 20;

    /** The curve, or a segment of it, is longer than the {@code max-set-length} of the config. */
    static final class TooLongException extends Exception {
        TooLongException() {
            super(null, null, false, false);
        }
    }

    /**
     * A step along a segment.
     *
     * @param point where the curve is
     * @param across the way to the left of the curve, on the xz plane and of length 1
     * @param distance how far along the curve the step is from the start of the segment
     * @param insideSide for a step that turns the cross section at a corner, the side of the curve inside the corner: 1
     * on the left, -1 on the right; 0 for a step along the segment
     */
    private record Step(Vector3 point, Vector3 across, double distance, int insideSide) {
        /**
         * Whether a column {@code side} blocks to the left of the curve is inside the corner that this step turns at,
         * where the step lays nothing.
         */
        boolean isInsideCorner(double side) {
            return insideSide * side > 0;
        }
    }

    /**
     * A segment added, to lay with the others.
     *
     * @param steps its steps, after those that turn the cross section at its start
     * @param start how far along the curve it starts
     * @param cornerBefore whether it meets the segment before at a corner
     */
    private record LaidSegment(List<Step> steps, double start, boolean cornerBefore) {
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
    /** The size of the selection along the axis, after which it repeats along the curve. */
    private final int repeat;
    private final int offset;
    private final boolean airOnly;
    private final Predicate<BlockVector3> isAir;
    private final int maxBlocks;
    private final int maxLength;
    private final int stepsPerBlock;

    /** The segments added so far, in order. */
    private final List<LaidSegment> segments = new ArrayList<>();
    /** The length of the segments added so far, where the next one goes on from. */
    private double addedLength;
    /** The way to the left of the curve at the end of the segments added so far, or null before the first. */
    private @Nullable Vector3 lastAcross;

    /** Whether {@link #blocks} has laid the segments. */
    private boolean laid;
    /** For each block to place, the block of the selection laid there first. */
    private final Map<BlockVector3, BlockVector3> sources = new HashMap<>();
    /**
     * For each xz cell, the segments, by their index, that meet another at a corner and have taken it.
     */
    private final Map<XzCell, Set<Integer>> xzCellsTaken = new HashMap<>();
    /** The blocks found not to be air, for {@code -a}, so that the world is read once for each block. */
    private final Set<BlockVector3> notAir = new HashSet<>();
    /** For each segment, by its index, the blocks of the row below its bottom row, where that row lays nothing. */
    private final List<Set<BlockVector3>> belowBottom = new ArrayList<>();

    /**
     * A layout with nothing laid yet. The selection is read along the axis of {@code arguments}, or without one along
     * the shorter of x and z, x when they are the same.
     *
     * @param selection the selection to lay along the curve
     * @param arguments the arguments of {@code //bc}
     * @param isAir whether the block at a position in the world is air, for {@code -a}
     * @param maxBlocks the most blocks to place, or 0 or less for no limit
     * @param maxLength the longest curve, from the {@code max-set-length} of the config
     * @param stepsPerBlock the steps along the curve per block, from the {@code fineness} of the config
     */
    CurveLayout(
            Selection selection,
            BcArguments arguments,
            Predicate<BlockVector3> isAir,
            int maxBlocks,
            int maxLength,
            int stepsPerBlock) {
        this.selection = selection;
        if (arguments.axis() != null) {
            this.axis = arguments.axis();
        } else {
            this.axis = selection.width() <= selection.length() ? BcOptions.Axis.X : BcOptions.Axis.Z;
        }
        this.columns = axis == BcOptions.Axis.X ? selection.length() : selection.width();
        this.repeat = axis == BcOptions.Axis.X ? selection.width() : selection.length();
        this.offset = arguments.offset();
        this.airOnly = arguments.air();
        this.isAir = isAir;
        this.maxBlocks = maxBlocks;
        this.maxLength = maxLength;
        this.stepsPerBlock = stepsPerBlock;
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

        double roughLength = segment.length(segment.fineness(ROUGH_STEPS_PER_BLOCK));
        // Two positions at the same place with no handles between them: nothing to place
        if (roughLength == 0) return;
        List<Step> along = steps(segment, (int) Math.ceil(stepsPerBlock * roughLength));
        double segmentLength = along.getLast().distance();
        if (addedLength + segmentLength > maxLength) throw new TooLongException();

        List<Step> steps = new ArrayList<>();
        boolean cornerBefore = false;
        if (lastAcross != null) {
            double turn = turn(lastAcross, along.getFirst().across());
            steps.addAll(corner(segment.p0(), lastAcross, turn));
            cornerBefore = Math.abs(turn) > CORNER;
        }
        steps.addAll(along);

        segments.add(new LaidSegment(steps, addedLength, cornerBefore));
        addedLength += segmentLength;
        lastAcross = along.getLast().across();
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
     * The length of the curve added so far, measured along its steps.
     *
     * @return the length in blocks
     */
    double length() {
        return addedLength;
    }

    /** Lays the cross section along every segment, in the order that the class describes. */
    private void layAll() throws MaxChangedBlocksException {
        for (LaidSegment segment : segments) {
            belowBottom.add(blocksBelowBottom(segment));
        }

        int[] columnsFromTheMiddle = fromTheMiddle(columns);
        for (int row = 0; row < selection.height(); row++) {
            for (int column : columnsFromTheMiddle) {
                for (int index = 0; index < segments.size(); index++) {
                    layColumn(index, row, column);
                }
            }
        }
    }

    /**
     * The start and end of {@code count} steps along {@code segment}, at even steps of t. The steps are not all equally
     * long along the curve: they are longer where it goes faster.
     */
    private static List<Step> steps(BezierSegment segment, int count) {
        List<Step> steps = new ArrayList<>();
        Vector3 before = segment.p0();
        double distance = 0;
        for (int step = 0; step <= count; step++) {
            double t = (double) step / count;
            Vector3 point = segment.point(t);
            distance += before.distance(point);
            steps.add(new Step(point, across(segment.tangent(t)), distance, 0));
            before = point;
        }
        return steps;
    }

    /**
     * The angle on the xz plane from {@code from} to {@code to}, the shorter way round: from -pi to pi, positive when
     * it turns from +x towards +z.
     */
    private static double turn(Vector3 from, Vector3 to) {
        return Math.atan2(from.x() * to.z() - from.z() * to.x(), from.x() * to.x() + from.z() * to.z());
    }

    /**
     * Steps at {@code point}, where a segment starts, that turn the cross section from {@code from}, the way across the
     * end of the segment before, by {@code angle}, to the way across the start of this one. They fill the outside of
     * the corner, with the outermost column moving at most a step of the curve between two; none when the angle is 0.
     */
    private List<Step> corner(Vector3 point, Vector3 from, double angle) {
        double outermost = (columns - 1) / 2.0;
        int turns = (int) Math.ceil(Math.abs(angle) * outermost * stepsPerBlock);
        // A positive angle, the way from +x to +z, turns the curve to the right: the inside of the corner is there
        int insideSide = angle > 0 ? -1 : 1;

        List<Step> corner = new ArrayList<>();
        for (int turn = 1; turn < turns; turn++) {
            double turned = angle * turn / turns;
            double cos = Math.cos(turned);
            double sin = Math.sin(turned);
            Vector3 across = Vector3.at(from.x() * cos - from.z() * sin, 0, from.x() * sin + from.z() * cos);
            corner.add(new Step(point, across, 0, insideSide));
        }
        return corner;
    }

    /**
     * The columns of a cross section in the order to lay them: from the middle outwards, so that where two columns
     * fall on the same block, the one nearer the middle keeps it. Of two columns as far from the middle, the lower
     * comes first: 5 columns give 2, 1, 3, 0, 4, and 4 columns give 1, 2, 0, 3.
     *
     * @param columns the number of columns across the selection
     * @return each column from 0 to {@code columns - 1} once
     */
    private static int[] fromTheMiddle(int columns) {
        double middle = (columns - 1) / 2.0;
        return IntStream.range(0, columns)
                .boxed()
                .sorted(Comparator.comparingDouble(column -> Math.abs(column - middle)))
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
        double middle = (columns - 1) / 2.0;
        return axis == BcOptions.Axis.X ? middle - column : column - middle;
    }

    /**
     * Lays one column of the cross section along segment {@code index}: the blocks of the selection along the axis, in
     * row {@code row} from the bottom and column {@code column} from the lowest coordinate across it. Each block the
     * column enters gets the block of the selection at that distance along the curve, unless {@link #takeXzCell} keeps
     * it from that xz cell, it is in the row below the bottom row of the segment, a block was laid there before, or
     * with {@code -a} it is not air.
     */
    private void layColumn(int index, int row, int column) throws MaxChangedBlocksException {
        LaidSegment segment = segments.get(index);
        double side = side(column);

        BlockVector3 before = null;
        for (Step step : segment.steps()) {
            if (step.isInsideCorner(side)) continue;
            BlockVector3 target = blockOf(step, side, row);
            if (target.equals(before)) continue;
            before = target;

            if (!takeXzCell(index, target)) continue;
            if (row == 0 && belowBottom.get(index).contains(target)) continue;
            if (sources.containsKey(target) || !mayPlace(target)) continue;
            sources.put(target, source(segment.start() + step.distance(), row, column));
            if (0 < maxBlocks && maxBlocks < sources.size()) {
                throw new MaxChangedBlocksException(maxBlocks);
            }
        }
    }

    /** The blocks that a row below the bottom row of the cross section passes along {@code segment}, in all columns. */
    private Set<BlockVector3> blocksBelowBottom(LaidSegment segment) {
        Set<BlockVector3> blocks = new HashSet<>();
        for (int column = 0; column < columns; column++) {
            double side = side(column);
            for (Step step : segment.steps()) {
                if (!step.isInsideCorner(side)) blocks.add(blockOf(step, side, -1));
            }
        }
        return blocks;
    }

    /** The block that the column {@code side} blocks to the left of the curve passes at {@code step} in {@code row}. */
    private static BlockVector3 blockOf(Step step, double side, int row) {
        return blockAt(step.point().add(step.across().multiply(side)).add(0, row, 0));
    }

    /** The block that {@code position} is in, the one with the higher coordinate on the edge between two. */
    private static BlockVector3 blockAt(Vector3 position) {
        return BlockVector3.floor(position.add(EDGE, EDGE, EDGE));
    }

    /**
     * Whether segment {@code index} may lay in the xz cell that {@code target} is in, taking it when it meets another
     * segment at a corner: false when a segment it meets at a corner has taken the xz cell first.
     */
    private boolean takeXzCell(int index, BlockVector3 target) {
        boolean cornerBefore = segments.get(index).cornerBefore();
        boolean cornerAfter = index + 1 < segments.size() && segments.get(index + 1).cornerBefore();
        if (!cornerBefore && !cornerAfter) return true;

        Set<Integer> taken = xzCellsTaken.computeIfAbsent(XzCell.of(target), key -> new HashSet<>());
        if (cornerBefore && taken.contains(index - 1) || cornerAfter && taken.contains(index + 1)) return false;
        taken.add(index);
        return true;
    }

    /** The block of the selection in {@code row} and {@code column} for {@code distance} along the curve. */
    private BlockVector3 source(double distance, int row, int column) {
        // How far into the repeating selection the block is
        int along = Math.floorMod((int) Math.floor(distance + offset + 0.5), repeat);
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
