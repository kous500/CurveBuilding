package me.kous500.curvebuilding.math;

import org.junit.jupiter.api.Test;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Vector3Test {
    @Test
    void blockCoordinatesRoundTowardNegativeInfinity() {
        Vector3 v = Vector3.at(-0.5, 1.9, -3.0);
        assertEquals(-1, v.blockX());
        assertEquals(1, v.blockY());
        assertEquals(-3, v.blockZ());
        assertEquals(BlockVector3.at(-1, 1, -3), v.toBlockPoint());
    }

    @Test
    void addAndSubtract() {
        Vector3 v = Vector3.at(1, 2, 3);
        assertVector(5, 7, 9, v.add(Vector3.at(4, 5, 6)));
        assertVector(0, 1, 2, v.add(-1, -1, -1));
        assertVector(-3, -3, -3, v.subtract(Vector3.at(4, 5, 6)));
    }

    @Test
    void distance() {
        assertEquals(5, Vector3.at(1, 2, 3).distance(Vector3.at(4, 6, 3)), 1e-9);
    }

    @Test
    void normalizeKeepsDirection() {
        assertVector(0.6, 0, 0.8, Vector3.at(3, 0, 4).normalize());
    }
}
