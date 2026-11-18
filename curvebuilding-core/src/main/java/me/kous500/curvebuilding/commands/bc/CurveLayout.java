package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.MaxChangedBlocksException;
import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Where {@code //bc} places which block of the selection along a curve. The selection is read along one axis: its
 * cross section across that axis is laid along the curve, turned with it and centered on it, with its bottom row at the
 * curve, and the selection repeats along the curve, starting {@code offset} blocks into it. Where the curve turns at a
 * point, the cross section turns around the point, filling the outside of the corner.
 *
 * <p>The cross section is laid row by row from the top, and in each row column by column from the middle outwards,
 * each column along every segment in order before the next. A block that several blocks of the selection fall on keeps
 * the first. Where two segments meet at a corner, turning by more than {@link #CORNER} on the xz plane, neither places
 * anything on a column of blocks, at any height, where the other has laid first: inside the corner, the two cross
 * sections meet where they are equally far from the middle, instead of crossing each other where the curve climbs or
 * falls.
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
     * @param turn 0 for a step along the segment; for a step that turns the cross section at a corner, 1 when it turns
     * towards the left of the curve and -1 when towards the right, so that the columns on that side, inside the
     * corner, are not laid there
     */
    private record Sample(Vector3 point, Vector3 across, double distance, int turn) {
    }

    /**
     * A segment added, to lay with the others.
     *
     * @param samples its steps, after those that turn the cross section at its start
     * @param start how far along the curve it starts
     * @param cornerBefore whether it meets the segment before at a corner
     */
    private record Added(List<Sample> samples, double start, boolean cornerBefore) {
    }

    private final Selection selection;
    private final BcOptions.Axis axis;
    private final int offset;
    private final boolean airOnly;
    private final Predicate<BlockVector3> isAir;
    private final int maxBlocks;
    private final int maxLength;
    private final int stepsPerBlock;

    /** The segments added so far, in order. */
    private final List<Added> added = new ArrayList<>();
    /** The length of the segments added so far, where the next one goes on from. */
    private double addedLength;
    /** The way to the left of the curve at the end of the segments added so far, or null before the first. */
    private @Nullable Vector3 lastAcross;

    /** Whether {@link #blocks} has laid the segments. */
    private boolean laid;
    /** For each block to place, the block of the selection laid there first. */
    private final NavigableMap<BlockVector3, BlockVector3> sources = new TreeMap<>();
    /**
     * For each column of blocks, by {@link #columnKey}, the segments that meet another at a corner and have laid on
     * it, by their index.
     */
    private final Map<Long, BitSet> columnsLaid = new HashMap<>();
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

        double roughLength = segment.length(segment.fineness(20));
        // Two positions at the same place with no handles between them: nothing to place
        if (roughLength == 0) return;
        List<Sample> steps = samples(segment, (int) Math.ceil(stepsPerBlock * roughLength));
        double segmentLength = steps.getLast().distance();
        if (addedLength + segmentLength > maxLength) throw new TooLongException();

        List<Sample> samples = new ArrayList<>();
        double turn = lastAcross == null ? 0 : turn(lastAcross, steps.getFirst().across());
        samples.addAll(corner(segment.p0(), lastAcross, turn));
        samples.addAll(steps);
        added.add(new Added(samples, addedLength, lastAcross != null && Math.abs(turn) > CORNER));
        addedLength += segmentLength;
        lastAcross = steps.getLast().across();
    }

    /**
     * The blocks to place and, for each, the block of the selection to place there. The first call lays the segments
     * added; after an exception, the layout is not to be used.
     *
     * @return a copy, in the order of the positions of the blocks to place
     * @throws MaxChangedBlocksException when the curve would place more than {@code maxBlocks} blocks
     */
    NavigableMap<BlockVector3, BlockVector3> blocks() throws MaxChangedBlocksException {
        if (!laid) {
            laid = true;
            int columns = columns();
            for (int row = selection.height() - 1; row >= 0; row--) {
                for (int column : fromTheMiddle(columns)) {
                    for (int index = 0; index < added.size(); index++) {
                        layColumn(index, row, column, columns);
                    }
                }
            }
        }
        return new TreeMap<>(sources);
    }

    /**
     * The length of the curve added so far, measured along its steps.
     *
     * @return the length in blocks
     */
    double length() {
        return addedLength;
    }

    /** The number of columns across the selection, the width of the cross section. */
    private int columns() {
        return axis == BcOptions.Axis.X ? selection.length() : selection.width();
    }

    /**
     * The start and end of {@code steps} steps along {@code segment}, at even steps of t. The steps are not all equally
     * long along the curve: they are longer where it goes faster.
     */
    private static List<Sample> samples(BezierSegment segment, int steps) {
        List<Sample> samples = new ArrayList<>();
        Vector3 before = segment.p0();
        double distance = 0;
        for (int step = 0; step <= steps; step++) {
            double t = (double) step / steps;
            Vector3 point = segment.point(t);
            distance += before.distance(point);
            samples.add(new Sample(point, across(segment.tangent(t)), distance, 0));
            before = point;
        }
        return samples;
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
     * the corner, with the outermost column moving at most a step of the curve between two; none for the first
     * segment, or when the angle is 0.
     */
    private List<Sample> corner(Vector3 point, @Nullable Vector3 from, double angle) {
        if (from == null) return List.of();
        double outermost = (columns() - 1) / 2.0;
        int turns = (int) Math.ceil(Math.abs(angle) * outermost * stepsPerBlock);

        List<Sample> corner = new ArrayList<>();
        for (int turn = 1; turn < turns; turn++) {
            double turned = angle * turn / turns;
            double cos = Math.cos(turned);
            double sin = Math.sin(turned);
            Vector3 across = Vector3.at(from.x() * cos - from.z() * sin, 0, from.x() * sin + from.z() * cos);
            corner.add(new Sample(point, across, 0, angle > 0 ? -1 : 1));
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
     * Lays one column of the cross section along segment {@code index}: the blocks of the selection along the axis, in
     * row {@code row} from the bottom and column {@code column} from the lowest coordinate across it. Each block the
     * column enters gets the block of the selection at that distance along the curve, unless a block was laid there
     * before, or a segment that meets this one at a corner has laid on that column of blocks.
     *
     * @param columns the number of columns across the selection
     */
    private void layColumn(int index, int row, int column, int columns) throws MaxChangedBlocksException {
        Added segment = added.get(index);
        boolean cornerAfter = index + 1 < added.size() && added.get(index + 1).cornerBefore();
        // Only a segment that meets another at a corner keeps the columns of blocks it lays on
        boolean atCorner = segment.cornerBefore() || cornerAfter;
        // How far to the left of the curve the column is. The selection is turned, not mirrored: read along x, its +z
        // is on the right of the curve; read along z, its +x is on the left.
        double middle = (columns - 1) / 2.0;
        double side = axis == BcOptions.Axis.X ? middle - column : column - middle;
        int repeat = axis == BcOptions.Axis.X ? selection.width() : selection.length();

        BlockVector3 before = null;
        for (Sample sample : segment.samples()) {
            if (sample.turn() * side > 0) continue;
            Vector3 position = sample.point().add(sample.across().multiply(side)).add(0, row, 0);
            BlockVector3 target = BlockVector3.floor(position.add(EDGE, EDGE, EDGE));
            if (target.equals(before)) continue;
            before = target;

            if (atCorner) {
                BitSet laidOnColumn = columnsLaid.computeIfAbsent(columnKey(target), key -> new BitSet());
                boolean otherLaidFirst = segment.cornerBefore() && laidOnColumn.get(index - 1)
                        || cornerAfter && laidOnColumn.get(index + 1);
                if (otherLaidFirst) continue;
                laidOnColumn.set(index);
            }

            if (sources.containsKey(target) || !mayPlace(target)) continue;
            sources.put(target, sourceAt(sample, segment.start(), row, column, repeat));
            if (0 < maxBlocks && maxBlocks < sources.size()) {
                throw new MaxChangedBlocksException(maxBlocks);
            }
        }
    }

    /** A key for the column of blocks, all along y, that {@code block} is in. */
    private static long columnKey(BlockVector3 block) {
        return (long) block.x() << 32 ^ (block.z() & 0xFFFFFFFFL);
    }

    /**
     * The block of the selection in {@code row} and {@code column} for where the curve is at {@code sample}, of a
     * segment that starts {@code start} along the curve.
     */
    private BlockVector3 sourceAt(Sample sample, double start, int row, int column, int repeat) {
        // How far into the repeating selection the block is
        int along = Math.floorMod((int) Math.floor(sample.distance() + start + offset + 0.5), repeat);
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
