package me.kous500.curvebuilding.commands.bc;

import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BlockWalkTest {
    /** Each block that the walk goes into after the first, with where it went in, as "x y z @ fraction". */
    private static List<String> walk(Vector3 from, Vector3 to) {
        BlockWalk walk = new BlockWalk(from, to);
        List<String> blocks = new ArrayList<>();
        while (walk.next()) {
            BlockVector3 block = walk.block();
            // Adding 0 turns -0, as a line going towards the lower coordinate crosses an edge at its start, into 0
            String entered = String.format(Locale.ROOT, "%.4f", walk.entered() + 0.0);
            blocks.add(block.x() + " " + block.y() + " " + block.z() + " @ " + entered);
        }
        return blocks;
    }

    @Test
    void startsInTheBlockOfItsStart() {
        BlockWalk walk = new BlockWalk(Vector3.at(-0.5, 64.2, 3.9), Vector3.at(-0.4, 64.3, 3.95));

        assertEquals(BlockVector3.at(-1, 64, 3), walk.block());
        assertEquals(0, walk.entered());
        assertFalse(walk.next());
    }

    @Test
    void goesThroughEachBlockAlongTheLine() {
        assertEquals(List.of("1 0 0 @ 0.1667", "2 0 0 @ 0.5000", "3 0 0 @ 0.8333"),
                walk(Vector3.at(0.5, 0.5, 0.5), Vector3.at(3.5, 0.5, 0.5)));
    }

    @Test
    void goesBackwards() {
        assertEquals(List.of("-1 0 0 @ 0.2500", "-2 0 0 @ 0.7500"),
                walk(Vector3.at(0.5, 0.5, 0.5), Vector3.at(-1.5, 0.5, 0.5)));
    }

    @Test
    void crossesXFirstWhereItCrossesTwoEdgesAtOnce() {
        assertEquals(List.of("1 0 0 @ 0.5000", "1 1 0 @ 0.5000"),
                walk(Vector3.at(0.5, 0.5, 0.5), Vector3.at(1.5, 1.5, 0.5)));
    }

    @Test
    void lineEndingOnAnEdgeStaysInTheBlockBeforeIt() {
        assertEquals(List.of("1 0 0 @ 0.3333"), walk(Vector3.at(0.5, 0.5, 0.5), Vector3.at(2, 0.5, 0.5)));
    }

    @Test
    void lineStartingOnAnEdgeTowardsTheLowerCoordinateCrossesItAtOnce() {
        BlockWalk walk = new BlockWalk(Vector3.at(1, 0.5, 0.5), Vector3.at(-0.5, 0.5, 0.5));

        assertEquals(BlockVector3.at(1, 0, 0), walk.block());
        assertEquals(List.of("0 0 0 @ 0.0000", "-1 0 0 @ 0.6667"),
                walk(Vector3.at(1, 0.5, 0.5), Vector3.at(-0.5, 0.5, 0.5)));
    }

    @Test
    void lineOfNoLengthStaysInItsBlock() {
        assertEquals(List.of(), walk(Vector3.at(2, 0.5, 0.5), Vector3.at(2, 0.5, 0.5)));
    }

    @Test
    void lineMovedBackCrossesTheSameEdgeLater() {
        BlockWalk walk = new BlockWalk(Vector3.at(0.75, 0.5, 0.5), Vector3.at(2.75, 0.5, 0.5));
        walk.next();

        // The line moved back by 0.25 goes from 0.5 to 2.5, and crosses x = 1 a quarter of the way
        assertEquals(0.125, walk.entered());
        assertEquals(0.25, walk.enteredWhenMovedBack(0.25));
    }
}
