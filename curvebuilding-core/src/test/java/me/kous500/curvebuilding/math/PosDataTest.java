package me.kous500.curvebuilding.math;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PosDataTest {
    // index of Vector3[] for each pos number
    private static final int POINT = 0;
    private static final int F = 1;
    private static final int B = 2;

    private PosData data;

    @BeforeEach
    void setUp() {
        data = new PosData();
    }

    private Vector3[] node(int n) {
        return data.getMap().get(n);
    }

    private void setPoints(int count) {
        for (int n = 1; n <= count; n++) {
            data.updatePos(n, POINT, Vector3.at((n - 1) * 10, 0, 0));
        }
    }

    @Test
    void movingPointMovesItsControlPoints() {
        setPoints(2);
        data.updatePos(2, F, Vector3.at(8, 0, 0));

        data.updatePos(2, POINT, Vector3.at(10, 5, 0));

        assertVector(10, 5, 0, node(2)[POINT]);
        assertVector(8, 5, 0, node(2)[F]);
    }

    @Test
    void settingFMirrorsBWhenNextPointExists() {
        setPoints(3);

        data.updatePos(2, F, Vector3.at(7, 1, 0));

        assertVector(13, -1, 0, node(2)[B]);
    }

    @Test
    void settingBMirrorsFWhenPreviousPointExists() {
        setPoints(3);

        data.updatePos(2, B, Vector3.at(14, 0, 0));

        assertVector(6, 0, 0, node(2)[F]);
    }

    @Test
    void mirroringKeepsLengthOfExistingControlPoint() {
        setPoints(3);
        data.updatePos(2, B, Vector3.at(14, 0, 0));

        data.updatePos(2, F, Vector3.at(10, 3, 0));

        // b points away from f but keeps its distance of 4 from the point
        assertVector(10, -4, 0, node(2)[B]);
    }

    @Test
    void controlPointIsNotMirroredAtEndOfCurve() {
        setPoints(2);

        data.updatePos(2, F, Vector3.at(8, 2, 0));

        assertNull(node(2)[B]);
    }

    @Test
    void addingNextPointCompletesPreviousB() {
        setPoints(2);
        data.updatePos(2, F, Vector3.at(8, 2, 0));

        data.updatePos(3, POINT, Vector3.at(20, 0, 0));

        assertVector(12, -2, 0, node(2)[B]);
    }

    @Test
    void clearingPointClearsWholeNode() {
        setPoints(2);
        data.updatePos(2, F, Vector3.at(8, 0, 0));

        assertTrue(data.clearSpecificPos(2, POINT));

        assertTrue(data.containsPos(2));
        assertArrayEquals(new Vector3[3], node(2));
    }

    @Test
    void clearingControlPointKeepsPoint() {
        setPoints(2);
        data.updatePos(2, F, Vector3.at(8, 0, 0));

        assertTrue(data.clearSpecificPos(2, F));

        assertVector(10, 0, 0, node(2)[POINT]);
        assertNull(node(2)[F]);
    }

    @Test
    void clearingMissingPosReturnsFalse() {
        assertFalse(data.clearSpecificPos(1, POINT));
    }

    @Test
    void insertShiftsLaterNodes() {
        setPoints(3);

        data.insertNodeAt(2);

        assertEquals(3, data.getMap().size());
        assertFalse(data.containsPos(2));
        assertVector(10, 0, 0, node(3)[POINT]);
        assertVector(20, 0, 0, node(4)[POINT]);
    }

    @Test
    void removeShiftsLaterNodes() {
        setPoints(3);

        data.removeNodeAt(2);

        assertEquals(2, data.getMap().size());
        assertVector(0, 0, 0, node(1)[POINT]);
        assertVector(20, 0, 0, node(2)[POINT]);
        assertFalse(data.containsPos(3));
    }

    @Test
    void shiftMovesEverySetPos() {
        setPoints(2);
        data.updatePos(2, F, Vector3.at(8, 0, 0));

        data.applyShift(Vector3.at(0, 3, -1));

        assertVector(0, 3, -1, node(1)[POINT]);
        assertVector(10, 3, -1, node(2)[POINT]);
        assertVector(8, 3, -1, node(2)[F]);
        assertNull(node(2)[B]);
    }
}
