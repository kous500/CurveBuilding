package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.MaxChangedBlocksException;
import me.kous500.curvebuilding.math.BezierSegment;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * Where {@code //bc} places which block of the selection along a curve, without touching the world. The selection is
 * read along one axis: its cross section across that axis is laid along the curve, turned with it, and the selection
 * repeats along the curve. A block that several blocks of the selection fall on gets the one that falls on it most.
 */
final class CurveLayout {
    /** The size of the selection, as WorldEdit gives it, and its center. */
    record Selection(int width, int height, int length, Vector3 center) {
    }

    /** The curve, or a segment of it, is longer than the {@code max-set-length} of the config. */
    static final class TooLongException extends Exception {
        TooLongException() {
            super(null, null, false, false);
        }
    }

    /** A step along a segment: where the curve is, which way it goes, and how far it is from the segment start. */
    private record Sample(Vector3 point, double angle, double distance) {
    }

    private final Selection selection;
    private final BcOptions.Axis axis;
    private final int every;
    private final int after;
    private final boolean airOnly;
    private final Predicate<BlockVector3> isAir;
    private final int maxBlocks;
    private final int maxLength;
    private final int stepsPerBlock;

    /** For each block to place, how often each block of the selection fell on it. */
    private final NavigableMap<BlockVector3, NavigableMap<BlockVector3, Integer>> votes = new TreeMap<>();
    /** The length of the segments laid so far, each counted in whole blocks, which the spacing of {@code -n} uses. */
    private int length;
    private double exactLength;

    /**
     * @param isAir whether the block at a position in the world is air, for {@code -a}
     * @param maxBlocks the most blocks to place, or 0 or less for no limit
     * @param maxLength the longest curve, from the {@code max-set-length} of the config
     * @param stepsPerBlock the steps along the curve per block, from the {@code fineness} of the config
     */
    CurveLayout(Selection selection, BcArguments arguments, Predicate<BlockVector3> isAir, int maxBlocks, int maxLength,
                int stepsPerBlock) {
        this.selection = selection;
        if (arguments.axis() != null) {
            this.axis = arguments.axis();
        } else {
            this.axis = selection.width() <= selection.length() ? BcOptions.Axis.X : BcOptions.Axis.Z;
        }
        this.every = arguments.n();
        this.after = arguments.m();
        this.airOnly = arguments.air();
        this.isAir = isAir;
        this.maxBlocks = maxBlocks;
        this.maxLength = maxLength;
        this.stepsPerBlock = stepsPerBlock;
    }

    /** Lays the selection along {@code segment}, after the segments laid before. */
    void add(BezierSegment segment) throws TooLongException, MaxChangedBlocksException {
        if (!segment.isWithin(maxLength)) throw new TooLongException();

        double segmentLength = segment.length(segment.fineness(20));
        // Two positions at the same place with no handles between them: nothing to place
        if (segmentLength == 0) return;
        double fineness = stepsPerBlock * segmentLength;
        double laidLength = segment.length(fineness);
        int newLength = length + (int) laidLength;
        if (newLength > maxLength) throw new TooLongException();

        List<Sample> samples = samples(segment, fineness);
        double below = Math.floor(selection.height() / 2.0 - 0.5);
        for (int y = 0; y < selection.height(); y++) {
            layRow(samples, y, selection.center().add(0, y - below, 0));
        }
        length = newLength;
        exactLength += laidLength;
    }

    /** For each block to place, the block of the selection to place there. */
    NavigableMap<BlockVector3, BlockVector3> blocks() {
        NavigableMap<BlockVector3, BlockVector3> blocks = new TreeMap<>();
        for (Map.Entry<BlockVector3, NavigableMap<BlockVector3, Integer>> entry : votes.entrySet()) {
            blocks.put(entry.getKey(), Collections.max(entry.getValue().entrySet(), Map.Entry.comparingByValue()).getKey());
        }
        return blocks;
    }

    /** The length of the curve laid so far. */
    double length() {
        return exactLength;
    }

    private static List<Sample> samples(BezierSegment segment, double fineness) {
        List<Sample> samples = new ArrayList<>();
        Vector3 before = segment.p0();
        double distance = 0;
        for (double t = 0; t <= 1; t += 1.0 / fineness) {
            // At 0 a handle can be at the point itself, where the curve has no direction yet
            double at = t == 0 ? 0.00001 : t;
            Vector3 point = segment.point(at);
            distance += Math.sqrt((point.x() - before.x()) * (point.x() - before.x())
                    + (point.y() - before.y()) * (point.y() - before.y())
                    + (point.z() - before.z()) * (point.z() - before.z()));
            samples.add(new Sample(point, segment.angle(at), distance));
            before = point;
        }
        return samples;
    }

    /**
     * Lays row {@code y} of the cross section, from its middle outwards on both sides. The middle column is laid from
     * both sides, so it counts twice.
     */
    private void layRow(List<Sample> samples, int y, Vector3 middle) throws MaxChangedBlocksException {
        Vector3 sideA = middle;
        Vector3 sideB = middle;
        if (axis == BcOptions.Axis.X) {
            int across = selection.length();
            for (int offset = 0; offset <= across / 2; offset++) {
                if (offset != across / 2 || across % 2 != 0) {
                    layColumn(samples, offset, y, sideA);
                    sideA = sideA.add(0, 0, -1);
                }
                layColumn(samples, -offset, y, sideB);
                sideB = sideB.add(0, 0, 1);
            }
        } else {
            int across = selection.width();
            for (int offset = 0; offset <= across / 2; offset++) {
                layColumn(samples, offset, y, sideA);
                sideA = sideA.add(1, 0, 0);
                if (offset != across / 2 || across % 2 != 0) {
                    layColumn(samples, -offset, y, sideB);
                    sideB = sideB.add(-1, 0, 0);
                }
            }
        }
    }

    /**
     * Lays one column of the cross section, {@code offset} blocks beside the curve and {@code y} blocks up, from the
     * blocks of the selection along the axis through {@code column}.
     */
    private void layColumn(List<Sample> samples, int offset, int y, Vector3 column) throws MaxChangedBlocksException {
        // With -n, the planes keep their spacing from one segment to the next
        int next = after;
        if (length != 0 && every != 0) next += length % every;

        BlockVector3 before = null;
        for (Sample sample : samples) {
            BlockVector3 target = BlockVector3.round(sample.point().add(
                    offset * Math.cos(-sample.angle()), y, offset * Math.sin(-sample.angle())));

            if (!target.equals(before) && sample.distance() >= next) {
                BlockVector3 source;
                if (axis == BcOptions.Axis.X) {
                    double along = ((sample.distance() + length + 0.5) % selection.width()) - (selection.width() / 2.0);
                    source = BlockVector3.floor(column.add(along + 0.5, 0, 0));
                } else {
                    double along = ((sample.distance() + length + 0.5) % selection.length()) - (selection.length() / 2.0);
                    source = BlockVector3.floor(column.add(0, 0, along + 0.5));
                }

                if (!airOnly || isAir.test(target)) {
                    votes.computeIfAbsent(target, k -> new TreeMap<>()).merge(source, 1, Integer::sum);
                }
                if (0 < maxBlocks && maxBlocks < votes.size()) {
                    throw new MaxChangedBlocksException(maxBlocks);
                }

                next += every;
            }
            before = target;
        }
    }
}
