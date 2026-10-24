package me.kous500.curvebuilding.math;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class VectorAssertions {
    private static final double DELTA = 1e-9;

    private VectorAssertions() {
    }

    public static void assertVector(double x, double y, double z, Vector3 actual) {
        assertNotNull(actual);
        assertAll(
                () -> assertEquals(x, actual.x(), DELTA, "x"),
                () -> assertEquals(y, actual.y(), DELTA, "y"),
                () -> assertEquals(z, actual.z(), DELTA, "z")
        );
    }
}
