package me.kous500.curvebuilding.preview;

import me.kous500.curvebuilding.math.CurvePoint;
import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The segments of straight curves along x, each 10 blocks long. */
class PreviewCurveTest {
    private static NavigableMap<Integer, CurvePoint> straight(int points) {
        NavigableMap<Integer, CurvePoint> map = new TreeMap<>();
        for (int n = 1; n <= points; n++) map.put(n, new CurvePoint(Vector3.at((n - 1) * 10, 64, 0), null, null));
        return map;
    }

    @Test
    void segmentsAreMeasuredFromTheStart() {
        List<PreviewCurve.ShownSegment> shown = PreviewCurve.segments(straight(3), 100, 20);

        assertEquals(2, shown.size());
        assertEquals(10, shown.get(0).length(), 1e-9);
        assertEquals(10, shown.get(0).lengthToEnd(), 1e-9);
        assertEquals(20, shown.get(1).lengthToEnd(), 1e-9);
    }

    @Test
    void curveStopsBeforeItGetsLongerThanTheMaximum() {
        assertEquals(2, PreviewCurve.segments(straight(4), 25, 20).size());
        assertEquals(3, PreviewCurve.segments(straight(4), 30, 20).size());
    }

    @Test
    void curveStopsAtASegmentWithAHandleTooFarAway() {
        NavigableMap<Integer, CurvePoint> curve = straight(3);
        curve.put(2, new CurvePoint(Vector3.at(10, 64, 0), null, Vector3.at(10, 64, 50)));

        // The second segment starts at point 2, whose handle b is 50 blocks away
        assertEquals(1, PreviewCurve.segments(curve, 40, 20).size());
    }
}
