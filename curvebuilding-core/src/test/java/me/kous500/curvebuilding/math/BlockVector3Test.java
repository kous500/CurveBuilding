package me.kous500.curvebuilding.math;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlockVector3Test {
    @Test
    void fromDoublesFloorsEachCoordinate() {
        assertEquals(BlockVector3.at(-1, 2, -4), BlockVector3.at(-0.5, 2.99, -3.01));
    }

    @Test
    void floorRoundAndCeil() {
        Vector3 v = Vector3.at(-1.5, 2.5, 0.2);
        assertEquals(BlockVector3.at(-2, 2, 0), BlockVector3.floor(v));
        assertEquals(BlockVector3.at(-1, 3, 0), BlockVector3.round(v));
        assertEquals(BlockVector3.at(-1, 3, 1), BlockVector3.ceil(v));
    }

    @Test
    void ordersByXThenYThenZ() {
        List<BlockVector3> vectors = new ArrayList<>(List.of(
                BlockVector3.at(1, 0, 0),
                BlockVector3.at(0, 1, 0),
                BlockVector3.at(0, 0, 1),
                BlockVector3.at(0, 0, -1)
        ));
        Collections.sort(vectors);
        assertEquals(List.of(
                BlockVector3.at(0, 0, -1),
                BlockVector3.at(0, 0, 1),
                BlockVector3.at(0, 1, 0),
                BlockVector3.at(1, 0, 0)
        ), vectors);
    }

    @Test
    void addAndSubtract() {
        BlockVector3 v = BlockVector3.at(1, 2, 3);
        assertEquals(BlockVector3.at(2, 4, 6), v.add(v));
        assertEquals(BlockVector3.at(0, 0, 0), v.subtract(1, 2, 3));
    }
}
