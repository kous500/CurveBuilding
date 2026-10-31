package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.MaxChangedBlocksException;
import me.kous500.curvebuilding.math.BezierSegment;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lays a selection along straight curves on y = 64, read along x, without a world. */
class CurveLayoutTest {
    private static final BcArguments NO_ARGUMENTS = new BcArguments(0, false, null);
    private static final Predicate<BlockVector3> NEVER_AIR = position -> false;

    // The centers are (min + max) / 2, as WorldEdit gives them

    /** A selection of one block, at the origin. */
    private static final CurveLayout.Selection ONE_BLOCK = new CurveLayout.Selection(1, 1, 1, Vector3.at(0, 0, 0));

    /** A selection of four blocks along x, from 0 to 3. */
    private static final CurveLayout.Selection FOUR_ALONG_X = new CurveLayout.Selection(4, 1, 1, Vector3.at(1.5, 0, 0));

    private static BezierSegment straight(Vector3 from, Vector3 to) {
        return new BezierSegment(from, from, to, to);
    }

    private static CurveLayout layout(CurveLayout.Selection selection, BcArguments arguments, int maxBlocks,
                                      BezierSegment... segments) throws Exception {
        CurveLayout layout = new CurveLayout(selection, arguments, NEVER_AIR, maxBlocks, 10000, 10);
        for (BezierSegment segment : segments) layout.add(segment);
        return layout;
    }

    private static Set<Integer> xs(NavigableMap<BlockVector3, BlockVector3> blocks) {
        Set<Integer> xs = new TreeSet<>();
        for (BlockVector3 target : blocks.keySet()) xs.add(target.x());
        return xs;
    }

    @Test
    void straightCurvePlacesOneBlockAtEachStep() throws Exception {
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(ONE_BLOCK, NO_ARGUMENTS, 0,
                straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0))).blocks();

        assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6), xs(blocks));
        for (BlockVector3 target : blocks.keySet()) {
            assertEquals(64, target.y());
            assertEquals(0, target.z());
        }
    }

    @Test
    void crossSectionIsLaidAcrossTheCurve() throws Exception {
        // Three blocks high and three wide across x, so the curve along x gets a 3 x 3 cross section
        CurveLayout.Selection selection = new CurveLayout.Selection(1, 3, 3, Vector3.at(0, 1, 1));
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(selection, NO_ARGUMENTS, 0,
                straight(Vector3.at(0, 64, 0), Vector3.at(4, 64, 0))).blocks();

        Set<Integer> ys = new TreeSet<>();
        Set<Integer> zs = new TreeSet<>();
        for (BlockVector3 target : blocks.keySet()) {
            ys.add(target.y());
            zs.add(target.z());
        }
        assertEquals(Set.of(64, 65, 66), ys);
        assertEquals(Set.of(-1, 0, 1), zs);
        assertEquals(5 * 9, blocks.size());
    }

    /** Which of the four blocks of {@link #FOUR_ALONG_X} each block of the curve gets, from the start of the curve. */
    private static List<Integer> blocksUsed(int offset, BezierSegment... segments) throws Exception {
        List<Integer> used = new ArrayList<>();
        BcArguments arguments = new BcArguments(offset, false, BcOptions.Axis.X);
        for (BlockVector3 source : layout(FOUR_ALONG_X, arguments, 0, segments).blocks().values()) {
            used.add(Math.floorMod(source.x(), 4));
        }
        return used;
    }

    @Test
    void selectionRepeatsAlongTheCurveFromItsOffset() throws Exception {
        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(9, 64, 0));

        assertEquals(List.of(0, 1, 2, 3, 0, 1, 2, 3, 0, 1), blocksUsed(0, line));
        assertEquals(List.of(2, 3, 0, 1, 2, 3, 0, 1, 2, 3), blocksUsed(2, line));
        // An offset of the whole selection or more goes around it
        assertEquals(blocksUsed(1, line), blocksUsed(5, line));
    }

    @Test
    void offsetCarriesOnFromOneSegmentToTheNext() throws Exception {
        assertEquals(List.of(1, 2, 3, 0, 1, 2, 3, 0, 1, 2), blocksUsed(1,
                straight(Vector3.at(0, 64, 0), Vector3.at(5, 64, 0)),
                straight(Vector3.at(5, 64, 0), Vector3.at(9, 64, 0))));
    }

    @Test
    void airOnlyPlacesNothingWhereThereIsNoAir() throws Exception {
        CurveLayout layout = new CurveLayout(ONE_BLOCK, new BcArguments(0, true, null), NEVER_AIR, 0, 10000, 10);
        layout.add(straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0)));

        assertTrue(layout.blocks().isEmpty());
    }

    @Test
    void limitCountsTheBlocksToPlace() throws Exception {
        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0));
        int blocks = layout(ONE_BLOCK, NO_ARGUMENTS, 0, line).blocks().size();

        // The middle column is laid twice, so counting each time a block falls on a position would stop here
        assertEquals(blocks, layout(ONE_BLOCK, NO_ARGUMENTS, blocks, line).blocks().size());
        assertThrows(MaxChangedBlocksException.class, () -> layout(ONE_BLOCK, NO_ARGUMENTS, blocks - 1, line));
    }

    @Test
    void segmentLongerThanTheMaximumIsRejected() {
        CurveLayout layout = new CurveLayout(ONE_BLOCK, NO_ARGUMENTS, NEVER_AIR, 0, 5, 10);

        assertThrows(CurveLayout.TooLongException.class, () -> layout.add(straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0))));
    }

    @Test
    void lengthAddsTheSegmentsWithoutRoundingThem() throws Exception {
        List<BezierSegment> diagonals = List.of(
                straight(Vector3.at(0, 64, 0), Vector3.at(1, 64, 1)),
                straight(Vector3.at(1, 64, 1), Vector3.at(2, 64, 2)));

        CurveLayout layout = layout(ONE_BLOCK, NO_ARGUMENTS, 0, diagonals.toArray(BezierSegment[]::new));

        // Each segment is measured in steps that stop just short of its end; a whole number of blocks each would give 2
        assertEquals(2 * Math.sqrt(2), layout.length(), 1e-2);
        assertFalse(layout.blocks().isEmpty());
    }
}
