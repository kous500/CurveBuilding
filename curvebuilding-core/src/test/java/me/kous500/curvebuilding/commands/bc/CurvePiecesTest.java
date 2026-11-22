package me.kous500.curvebuilding.commands.bc;

import me.kous500.curvebuilding.commands.bc.CurvePieces.Vertex;
import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static me.kous500.curvebuilding.math.VectorAssertions.assertVector;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurvePiecesTest {
    private static final Vector3 TOWARDS_X = Vector3.at(1, 0, 0);

    @Test
    void straightSegmentIsOnePiece() {
        Vector3 from = Vector3.at(0.5, 64.5, 0.5);
        Vector3 to = Vector3.at(10.5, 64.5, 0.5);

        List<Vertex> vertices = new CurvePieces(4).along(new BezierSegment(from, from, to, to));

        assertEquals(2, vertices.size());
        assertVector(10.5, 64.5, 0.5, vertices.getLast().point());
        assertEquals(10, vertices.getLast().distance(), 1e-12);
        // The left of a curve towards +x is towards -z
        assertVector(0, 0, -1, vertices.getFirst().across());
    }

    @Test
    void lengthIsMeasuredWhereTheCurveTurnsBackOnItself() {
        // Straight up, some way back down and up again: the speed falls to nothing at each of the two turns
        BezierSegment upAndDown = new BezierSegment(
                Vector3.at(0.5, 64.5, 0.5),
                Vector3.at(0.5, 104.5, 0.5),
                Vector3.at(0.5, 54.5, 0.5),
                Vector3.at(0.5, 84.5, 0.5));

        List<Vertex> vertices = new CurvePieces(0).along(upAndDown);

        assertEquals(upAndDown.length(1_000_000), vertices.getLast().distance(), 1e-6);
    }

    @Test
    void wideCrossSectionTurnsAroundACornerInMorePieces() {
        Vector3 point = Vector3.at(8, 64, 0);

        // The outermost columns, 4 blocks from the middle, stay within the tolerance along pieces of about 18 degrees
        List<Vertex> wide = new CurvePieces(4).corner(point, TOWARDS_X, Math.PI / 2);
        List<Vertex> single = new CurvePieces(0).corner(point, TOWARDS_X, Math.PI / 2);

        assertEquals(5, wide.size());
        assertEquals(1, single.size());
        assertVector(1, 0, 0, wide.getFirst().across());
        assertTrue(wide.stream().allMatch(vertex -> vertex.distance() == 0 && vertex.point().equals(point)));
        // Turning towards +z turns the curve to the right, so the right is inside the corner
        assertTrue(wide.getFirst().isInsideCorner(-1));
    }

    @Test
    void noTurnMakesNoCorner() {
        assertEquals(List.of(), new CurvePieces(4).corner(Vector3.at(8, 64, 0), TOWARDS_X, 0));
    }

    @Test
    void turnIsPositiveFromXTowardsZ() {
        assertEquals(Math.PI / 2, CurvePieces.turn(TOWARDS_X, Vector3.at(0, 5, 3)), 1e-12);
        assertEquals(-Math.PI / 2, CurvePieces.turn(TOWARDS_X, Vector3.at(0, 0, -1)), 1e-12);
    }
}
