package me.kous500.curvebuilding.preview;

import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.curve.CurvePoint;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

/** The part of a curve that a preview shows: its segments from the start, as long as the curve stays short enough. */
public final class PreviewCurve {
    private PreviewCurve() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * A segment that is shown, with how long it is.
     *
     * @param segment the segment
     * @param length its length, in blocks
     * @param lengthToEnd the length of the curve from its start to the end of this segment, in blocks
     */
    public record ShownSegment(BezierSegment segment, double length, double lengthToEnd) {
    }

    /**
     * The segments through {@code points} that are shown. They stop before the first segment whose ends, or an end and
     * its handle, are more than {@code maxLength} apart, and before the curve gets longer than {@code maxLength}.
     *
     * @param points the points of the curve by their number
     * @param maxLength the longest curve shown, in blocks
     * @param stepsPerBlock the steps per block to measure the segments with, as {@link BezierSegment#fineness} takes
     * @return the shown segments, in order
     */
    public static List<ShownSegment> segments(
            NavigableMap<Integer, CurvePoint> points,
            double maxLength,
            double stepsPerBlock) {
        List<ShownSegment> shown = new ArrayList<>();
        double lengthToEnd = 0;
        for (BezierSegment segment : BezierSegment.through(points)) {
            if (!segment.isWithin(maxLength)) break;
            double length = segment.length(segment.fineness(stepsPerBlock));
            lengthToEnd = lengthToEnd + length;
            if (lengthToEnd > maxLength) break;
            shown.add(new ShownSegment(segment, length, lengthToEnd));
        }
        return shown;
    }
}
