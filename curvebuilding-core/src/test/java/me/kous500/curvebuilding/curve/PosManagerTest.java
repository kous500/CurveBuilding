package me.kous500.curvebuilding.curve;

import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;

class PosManagerTest {
    @ParameterizedTest
    @CsvSource({
            // The middle half of a block goes to its center, and the quarter at each side to that edge
            "10.0, 10.0",
            "10.2, 10.0",
            "10.25, 10.5",
            "10.5, 10.5",
            "10.7, 10.5",
            "10.75, 11.0",
            "10.9, 11.0",
            "-0.2, 0.0",
            "-0.25, 0.0",
            "-0.3, -0.5",
            "-0.75, -0.5",
            "-0.8, -1.0",
    })
    void standingPositionGoesToTheNearestHalfBlockOnXAndZ(double feet, double point) {
        assertVector(point, 64.5, point, PosManager.standingPosition(Vector3.at(feet, 64.2, feet)));
    }

    @ParameterizedTest
    @CsvSource({"64.0, 64.5", "64.99, 64.5", "-0.2, -0.5"})
    void standingPositionIsInTheMiddleOfTheBlockOfTheFeetOnY(double feet, double point) {
        assertVector(10.5, point, 10.5, PosManager.standingPosition(Vector3.at(10.5, feet, 10.5)));
    }
}
