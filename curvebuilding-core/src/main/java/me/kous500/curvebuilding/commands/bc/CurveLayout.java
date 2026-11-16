package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.MaxChangedBlocksException;
import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Where {@code //bc} places which block of the selection along a curve. The selection is read along one axis: its
 * cross section across that axis is laid along the curve, turned with it and centered on it, with its bottom row at the
 * curve, and the selection repeats along the curve, starting {@code offset} blocks into it.
 *
 * <p>A block that several blocks of the selection fall on keeps the first: the one from an earlier segment, then from
 * a lower row, then from a column nearer the middle of the cross section, then from earlier along the curve.
 *
 * <p>Add the segments of the curve in order with {@link #add}, then take the blocks with {@link #blocks}. The world is
 * only read through {@code isAir}, for {@code -a}, and nothing is placed.
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
     */
    private record Sample(Vector3 point, Vector3 across, double distance) {
    }

    private final Selection selection;
    private final BcOptions.Axis axis;
    private final int offset;
    private final boolean airOnly;
    private final Predicate<BlockVector3> isAir;
    private final int maxBlocks;
    private final int maxLength;
    private final int stepsPerBlock;

    /** For each block to place, the block of the selection that fell on it first. */
    private final NavigableMap<BlockVector3, BlockVector3> sources = new TreeMap<>();
    /** The blocks found not to be air, for {@code -a}, so that the world is read once for each block. */
    private final Set<BlockVector3> notAir = new HashSet<>();
    /** The length of the segments laid so far, where the selection goes on from. */
    private double laidLength;

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
     * Lays the selection along {@code segment}, going on from the segments laid before. After an exception, the layout
     * is left half laid and is not to be used.
     *
     * @param segment the next segment of the curve
     * @throws TooLongException when the ends of the segment, or an end and its handle, are further apart than
     * {@code maxLength}, or the curve with the segment is longer than it
     * @throws MaxChangedBlocksException when the curve would place more than {@code maxBlocks} blocks
     */
    void add(BezierSegment segment) throws TooLongException, MaxChangedBlocksException {
        if (!segment.isWithin(maxLength)) throw new TooLongException();

        double roughLength = segment.length(segment.fineness(20));
        // Two positions at the same place with no handles between them: nothing to place
        if (roughLength == 0) return;
        List<Sample> samples = samples(segment, (int) Math.ceil(stepsPerBlock * roughLength));
        double segmentLength = samples.getLast().distance();
        if (laidLength + segmentLength > maxLength) throw new TooLongException();

        int columns = axis == BcOptions.Axis.X ? selection.length() : selection.width();
        int[] columnsFromTheMiddle = fromTheMiddle(columns);
        for (int row = 0; row < selection.height(); row++) {
            for (int column : columnsFromTheMiddle) {
                layColumn(samples, row, column, columns);
            }
        }
        laidLength += segmentLength;
    }

    /**
     * The blocks to place and, for each, the block of the selection to place there.
     *
     * @return a copy, in the order of the positions of the blocks to place
     */
    NavigableMap<BlockVector3, BlockVector3> blocks() {
        return new TreeMap<>(sources);
    }

    /**
     * The length of the curve laid so far, measured along its steps.
     *
     * @return the length in blocks
     */
    double length() {
        return laidLength;
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
            samples.add(new Sample(point, across(segment.tangent(t)), distance));
            before = point;
        }
        return samples;
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
     * Lays one column of the cross section along the steps of a segment: the blocks of the selection along the axis,
     * in row {@code row} from the bottom and column {@code column} from the lowest coordinate across it. When the
     * column enters a block that has none yet, it gets the block of the selection at that distance along the curve.
     *
     * @param columns the number of columns across the selection
     */
    private void layColumn(List<Sample> samples, int row, int column, int columns) throws MaxChangedBlocksException {
        // How far to the left of the curve the column is. The selection is turned, not mirrored: read along x, its +z
        // is on the right of the curve; read along z, its +x is on the left.
        double middle = (columns - 1) / 2.0;
        double side = axis == BcOptions.Axis.X ? middle - column : column - middle;
        int repeat = axis == BcOptions.Axis.X ? selection.width() : selection.length();

        BlockVector3 before = null;
        for (Sample sample : samples) {
            Vector3 position = sample.point().add(sample.across().multiply(side)).add(0, row, 0);
            BlockVector3 target = BlockVector3.floor(position.add(EDGE, EDGE, EDGE));

            if (!target.equals(before) && !sources.containsKey(target) && mayPlace(target)) {
                // How far into the repeating selection this block is
                int along = Math.floorMod((int) Math.floor(sample.distance() + laidLength + offset + 0.5), repeat);
                BlockVector3 source = axis == BcOptions.Axis.X
                        ? selection.min().add(along, row, column)
                        : selection.min().add(column, row, along);

                sources.put(target, source);
                if (0 < maxBlocks && maxBlocks < sources.size()) {
                    throw new MaxChangedBlocksException(maxBlocks);
                }
            }
            before = target;
        }
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
