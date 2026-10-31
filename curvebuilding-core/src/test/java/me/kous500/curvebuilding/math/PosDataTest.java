package me.kous500.curvebuilding.math;

import me.kous500.curvebuilding.math.CurvePoint.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NavigableMap;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosDataTest {
    private PosData data;

    @BeforeEach
    void setUp() {
        data = new PosData();
    }

    private CurvePoint node(int n) {
        return data.points().get(n);
    }

    private void setPoints(int count) {
        for (int n = 1; n <= count; n++) {
            data.set(n, Part.POINT, Vector3.at((n - 1) * 10, 0, 0));
        }
    }

    @Test
    void movingPointMovesItsControlPoints() {
        setPoints(2);
        data.set(2, Part.F, Vector3.at(8, 0, 0));

        data.set(2, Part.POINT, Vector3.at(10, 5, 0));

        assertVector(10, 5, 0, node(2).point());
        assertVector(8, 5, 0, node(2).f());
    }

    @Test
    void settingFMirrorsBWhenNextPointExists() {
        setPoints(3);

        data.set(2, Part.F, Vector3.at(7, 1, 0));

        assertVector(13, -1, 0, node(2).b());
    }

    @Test
    void settingBMirrorsFWhenPreviousPointExists() {
        setPoints(3);

        data.set(2, Part.B, Vector3.at(14, 0, 0));

        assertVector(6, 0, 0, node(2).f());
    }

    @Test
    void mirroringKeepsLengthOfExistingControlPoint() {
        setPoints(3);
        data.set(2, Part.B, Vector3.at(14, 0, 0));

        data.set(2, Part.F, Vector3.at(10, 3, 0));

        // b points away from f but keeps its distance of 4 from the point
        assertVector(10, -4, 0, node(2).b());
    }

    @Test
    void controlPointIsNotMirroredAtEndOfCurve() {
        setPoints(2);

        data.set(2, Part.F, Vector3.at(8, 2, 0));

        assertNull(node(2).b());
    }

    @Test
    void addingNextPointCompletesPreviousB() {
        setPoints(2);
        data.set(2, Part.F, Vector3.at(8, 2, 0));

        data.set(3, Part.POINT, Vector3.at(20, 0, 0));

        assertVector(12, -2, 0, node(2).b());
    }

    @Test
    void clearingPointClearsWholeNode() {
        setPoints(2);
        data.set(2, Part.F, Vector3.at(8, 0, 0));

        assertTrue(data.clear(2, Part.POINT));

        assertTrue(data.contains(2));
        assertEquals(CurvePoint.EMPTY, node(2));
    }

    @Test
    void clearingControlPointKeepsPoint() {
        setPoints(2);
        data.set(2, Part.F, Vector3.at(8, 0, 0));

        assertTrue(data.clear(2, Part.F));

        assertVector(10, 0, 0, node(2).point());
        assertNull(node(2).f());
    }

    @Test
    void clearingMissingPosReturnsFalse() {
        assertFalse(data.clear(1, Part.POINT));
    }

    @Test
    void insertShiftsLaterNodes() {
        setPoints(3);

        data.insertAt(2);

        assertEquals(3, data.points().size());
        assertFalse(data.contains(2));
        assertVector(10, 0, 0, node(3).point());
        assertVector(20, 0, 0, node(4).point());
    }

    @Test
    void removeShiftsLaterNodes() {
        setPoints(3);

        data.removeAt(2);

        assertEquals(2, data.points().size());
        assertVector(0, 0, 0, node(1).point());
        assertVector(20, 0, 0, node(2).point());
        assertFalse(data.contains(3));
    }

    @Test
    void shiftMovesEverySetPos() {
        setPoints(2);
        data.set(2, Part.F, Vector3.at(8, 0, 0));

        data.move(Vector3.at(0, 3, -1));

        assertVector(0, 3, -1, node(1).point());
        assertVector(10, 3, -1, node(2).point());
        assertVector(8, 3, -1, node(2).f());
        assertNull(node(2).b());
    }

    @Test
    void pointsReadBeforeAChangeStayAsTheyWere() {
        setPoints(2);
        NavigableMap<Integer, CurvePoint> before = data.points();

        data.move(Vector3.at(0, 3, 0));
        data.removeAt(1);

        assertEquals(2, before.size());
        assertVector(10, 0, 0, before.get(2).point());
        assertThrows(UnsupportedOperationException.class, () -> before.put(3, CurvePoint.EMPTY));
    }
}
