package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.MaxChangedBlocksException;
import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
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

    /** The corner of every selection here. */
    private static final BlockVector3 ORIGIN = BlockVector3.at(0, 0, 0);

    /** A selection of one block, at the origin. */
    private static final CurveLayout.Selection ONE_BLOCK = new CurveLayout.Selection(1, 1, 1, ORIGIN);

    /** A selection of four blocks along x, from 0 to 3. */
    private static final CurveLayout.Selection FOUR_ALONG_X = new CurveLayout.Selection(4, 1, 1, ORIGIN);

    /**
     * A straight segment without handles from the center of block {@code from} to that of block {@code to}, as
     * {@code //bcpos} sets points where a player stands in the middle of a block.
     */
    private static BezierSegment straight(Vector3 from, Vector3 to) {
        Vector3 start = from.add(0.5, 0.5, 0.5);
        Vector3 end = to.add(0.5, 0.5, 0.5);
        return new BezierSegment(start, start, end, end);
    }

    private static CurveLayout layout(
            CurveLayout.Selection selection,
            BcArguments arguments,
            int maxBlocks,
            BezierSegment... segments) throws Exception {
        CurveLayout layout = new CurveLayout(selection, arguments, NEVER_AIR, maxBlocks, 10000, 10);
        for (BezierSegment segment : segments) {
            layout.add(segment);
        }
        return layout;
    }

    private static Set<Integer> xs(NavigableMap<BlockVector3, BlockVector3> blocks) {
        Set<Integer> xs = new TreeSet<>();
        for (BlockVector3 target : blocks.keySet()) {
            xs.add(target.x());
        }
        return xs;
    }

    @Test
    void straightCurvePlacesOneBlockAtEachStep() throws Exception {
        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0));
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(ONE_BLOCK, NO_ARGUMENTS, 0, line).blocks();

        assertEquals(Set.of(0, 1, 2, 3, 4, 5, 6), xs(blocks));
        for (BlockVector3 target : blocks.keySet()) {
            assertEquals(64, target.y());
            assertEquals(0, target.z());
        }
    }

    @Test
    void crossSectionIsLaidAcrossTheCurve() throws Exception {
        // Three blocks high and three wide across x, so the curve along x gets a 3 x 3 cross section
        CurveLayout.Selection selection = new CurveLayout.Selection(1, 3, 3, ORIGIN);
        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(4, 64, 0));
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(selection, NO_ARGUMENTS, 0, line).blocks();

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

    @Test
    void crossSectionIsAcrossTheCurveAtItsEnd() throws Exception {
        // With one step per block, the last step is at the end, where the segment has no handle
        CurveLayout.Selection selection = new CurveLayout.Selection(1, 1, 3, ORIGIN);
        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(4, 64, 0));
        CurveLayout layout = new CurveLayout(selection, NO_ARGUMENTS, NEVER_AIR, 0, 10000, 1);
        layout.add(line);
        NavigableMap<BlockVector3, BlockVector3> blocks = layout.blocks();

        assertEquals(Set.of(0, 1, 2, 3, 4), xs(blocks));
        assertEquals(5 * 3, blocks.size());
    }

    @Test
    void crossSectionTurnsWithTheCurve() throws Exception {
        CurveLayout.Selection selection = new CurveLayout.Selection(1, 3, 3, ORIGIN);
        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(0, 64, 4));
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(selection, NO_ARGUMENTS, 0, line).blocks();

        assertEquals(Set.of(-1, 0, 1), xs(blocks));
        assertEquals(5 * 9, blocks.size());
    }

    /**
     * At x = 4, the z of each block placed and of the block of the selection it gets, from a straight curve along x at
     * {@code z}. The selection is four blocks across x, with z from 0 to 3.
     */
    private static NavigableMap<Integer, Integer> fourAcross(double z, boolean towardsPlusX) throws Exception {
        CurveLayout.Selection selection = new CurveLayout.Selection(1, 1, 4, ORIGIN);
        Vector3 west = Vector3.at(0.5, 64.5, z);
        Vector3 east = Vector3.at(8.5, 64.5, z);
        BezierSegment line = towardsPlusX
                ? new BezierSegment(west, west, east, east)
                : new BezierSegment(east, east, west, west);

        NavigableMap<Integer, Integer> sources = new TreeMap<>();
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(selection, NO_ARGUMENTS, 0, line).blocks();
        for (Map.Entry<BlockVector3, BlockVector3> block : blocks.entrySet()) {
            if (block.getKey().x() == 4) sources.put(block.getKey().z(), block.getValue().z());
        }
        return sources;
    }

    @Test
    void evenCrossSectionIsCenteredOnACurveAlongTheEdgeOfBlocks() throws Exception {
        // Along the edge between z = 0 and z = 1: two columns on each side, turned half a turn the other way
        assertEquals(Map.of(-1, 0, 0, 1, 1, 2, 2, 3), fourAcross(1.0, true));
        assertEquals(Map.of(-1, 3, 0, 2, 1, 1, 2, 0), fourAcross(1.0, false));
    }

    @Test
    void evenCrossSectionAlongTheMiddleOfBlocksHasItsExtraColumnTowardsPlusZ() throws Exception {
        // Its columns are on the edges of blocks, and each goes to the block with the higher z
        assertEquals(Map.of(-1, 0, 0, 1, 1, 2, 2, 3), fourAcross(0.5, true));
        assertEquals(Map.of(-1, 3, 0, 2, 1, 1, 2, 0), fourAcross(0.5, false));
    }

    @Test
    void evenCrossSectionAlongTheMiddleOfBlocksHasFourBlocksAtEachStep() throws Exception {
        CurveLayout.Selection selection = new CurveLayout.Selection(1, 1, 4, ORIGIN);
        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(40, 64, 0));
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(selection, NO_ARGUMENTS, 0, line).blocks();

        assertEquals(41 * 4, blocks.size());
        for (BlockVector3 target : blocks.keySet()) {
            assertTrue(-1 <= target.z() && target.z() <= 2, target.toString());
        }
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
        assertEquals(List.of(3, 0, 1, 2, 3, 0, 1, 2, 3, 0), blocksUsed(-1, line));
        // An offset of the whole selection or more goes around it, either way
        assertEquals(blocksUsed(1, line), blocksUsed(5, line));
        assertEquals(blocksUsed(-1, line), blocksUsed(-5, line));
        assertEquals(blocksUsed(-1, line), blocksUsed(3, line));
    }

    @Test
    void offsetCarriesOnFromOneSegmentToTheNext() throws Exception {
        BezierSegment first = straight(Vector3.at(0, 64, 0), Vector3.at(5, 64, 0));
        BezierSegment second = straight(Vector3.at(5, 64, 0), Vector3.at(9, 64, 0));
        assertEquals(List.of(1, 2, 3, 0, 1, 2, 3, 0, 1, 2), blocksUsed(1, first, second));
    }

    /** Straight segments along x on y = 64, {@code length} blocks each, end to end from the middle of block 0. */
    private static BezierSegment[] segmentsOf(double length, int count) {
        BezierSegment[] segments = new BezierSegment[count];
        for (int i = 0; i < count; i++) {
            Vector3 from = Vector3.at(0.5 + length * i, 64.5, 0.5);
            Vector3 to = Vector3.at(0.5 + length * (i + 1), 64.5, 0.5);
            segments[i] = new BezierSegment(from, from, to, to);
        }
        return segments;
    }

    @Test
    void selectionGoesOnAcrossSegmentsOfAnyLength() throws Exception {
        BezierSegment[] whole = segmentsOf(19, 1);
        BezierSegment[] pieces = segmentsOf(1.9, 10);

        assertEquals(List.of(0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3, 0, 1, 2, 3), blocksUsed(0, whole));
        assertEquals(blocksUsed(0, whole), blocksUsed(0, pieces));
    }

    @Test
    void airOnlyPlacesNothingWhereThereIsNoAir() throws Exception {
        CurveLayout layout = new CurveLayout(ONE_BLOCK, new BcArguments(0, true, null), NEVER_AIR, 0, 10000, 10);
        layout.add(straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0)));

        assertTrue(layout.blocks().isEmpty());
    }

    @Test
    void airOnlyReadsEachBlockOfTheWorldOnce() throws Exception {
        // Air where z = 1 only, over a curve that goes along x and back, three blocks wide
        Map<BlockVector3, Integer> reads = new TreeMap<>();
        Predicate<BlockVector3> isAir = position -> {
            reads.merge(position, 1, Integer::sum);
            return position.z() == 1;
        };
        CurveLayout.Selection selection = new CurveLayout.Selection(1, 1, 3, ORIGIN);
        CurveLayout layout = new CurveLayout(selection, new BcArguments(0, true, null), isAir, 0, 10000, 10);
        layout.add(straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0)));
        layout.add(straight(Vector3.at(6, 64, 0), Vector3.at(0, 64, 0)));
        Set<BlockVector3> placed = layout.blocks().keySet();

        assertEquals(Set.of(1), Set.copyOf(reads.values()));
        Set<BlockVector3> air = new TreeSet<>();
        for (BlockVector3 position : reads.keySet()) {
            if (position.z() == 1) air.add(position);
        }
        assertEquals(air, placed);
    }

    /** How many blocks each column of a road five blocks wide, read along x, gives along {@code segments}. */
    private static List<Integer> columnsUsed(BezierSegment... segments) throws Exception {
        CurveLayout.Selection road = new CurveLayout.Selection(1, 1, 5, ORIGIN);
        Integer[] used = {0, 0, 0, 0, 0};
        BcArguments arguments = new BcArguments(0, false, BcOptions.Axis.X);
        for (BlockVector3 source : layout(road, arguments, 0, segments).blocks().values()) {
            used[source.z()]++;
        }
        return List.of(used);
    }

    @Test
    void middleOfTheCrossSectionKeepsTheBlocksItSharesWithTheColumnsBeside() throws Exception {
        // On a slant, neighboring columns fall on the same blocks; the middle line and both sides stay even
        List<Integer> used = columnsUsed(straight(Vector3.at(0, 64, 0), Vector3.at(40, 64, 30)));

        assertEquals(List.of(51, 51, 71, 51, 51), used);
    }

    @Test
    void cornerInsideATurnIsSplitBetweenTheSegmentsOnTheirNearerSide() throws Exception {
        // A turn to the left: from x = 6 to 8 and z = -2 to 0, each block takes the column of the segment whose column
        // there is nearer its middle; where both are as near, they give the same column
        NavigableMap<BlockVector3, BlockVector3> blocks = turnLeft();
        int[][] columns = {
                {0, 1, 2}, // z = -2
                {1, 1, 2}, // z = -1
                {2, 2, 2}, // z = 0
        };

        for (int z = -2; z <= 0; z++) {
            for (int x = 6; x <= 8; x++) {
                assertEquals(columns[z + 2][x - 6], blocks.get(BlockVector3.at(x, 64, z)).z(), x + ", " + z);
            }
        }
    }

    @Test
    void cornerOutsideATurnIsFilledAroundThePoint() throws Exception {
        // The cross section turns around (8.5, 0.5); beyond its reach of two and a half blocks, the corner stays empty
        NavigableMap<BlockVector3, BlockVector3> blocks = turnLeft();

        assertTrue(blocks.containsKey(BlockVector3.at(9, 64, 1)));
        assertTrue(blocks.containsKey(BlockVector3.at(9, 64, 2)));
        assertTrue(blocks.containsKey(BlockVector3.at(10, 64, 1)));
        assertFalse(blocks.containsKey(BlockVector3.at(10, 64, 2)));
    }

    @Test
    void cornerOfACurveThatFallsHasOneLayerInside() throws Exception {
        // Flat along +x, then down along -z, one block for each: inside the turn, each block keeps the height of the
        // segment that takes it, with nothing of the other above or below
        CurveLayout.Selection road = new CurveLayout.Selection(1, 1, 5, ORIGIN);
        BezierSegment flat = straight(Vector3.at(0, 64, 0), Vector3.at(8, 64, 0));
        BezierSegment down = straight(Vector3.at(8, 64, 0), Vector3.at(8, 56, -8));
        NavigableMap<BlockVector3, BlockVector3> blocks =
                layout(road, new BcArguments(0, false, BcOptions.Axis.X), 0, flat, down).blocks();

        // On the side of the flat segment, the road stays at y = 64
        for (int[] xz : new int[][] {{6, 0}, {7, 0}, {6, -1}}) {
            for (BlockVector3 target : blocks.keySet()) {
                if (target.x() == xz[0] && target.z() == xz[1]) assertEquals(64, target.y(), target.toString());
            }
        }
        // On the side of the segment going down, nothing is left at y = 64
        for (int[] xz : new int[][] {{7, -2}, {8, -2}, {8, -1}}) {
            assertFalse(blocks.containsKey(BlockVector3.at(xz[0], 64, xz[1])), xz[0] + ", " + xz[1]);
        }
    }

    @Test
    void segmentsGoingOnTheSameWayKeepTheBlocksOfBoth() throws Exception {
        // Flat along +x to x = 8.5, then on the same way up two blocks for each: not a corner, so the steep segment
        // also lays on the last column of blocks of the flat one, at y = 65
        BezierSegment flat = straight(Vector3.at(0, 64, 0), Vector3.at(8, 64, 0));
        BezierSegment steep = straight(Vector3.at(8, 64, 0), Vector3.at(16, 80, 0));
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(ONE_BLOCK, NO_ARGUMENTS, 0, flat, steep).blocks();

        assertTrue(blocks.containsKey(BlockVector3.at(8, 64, 0)));
        assertTrue(blocks.containsKey(BlockVector3.at(8, 65, 0)));
    }

    /** A road five blocks wide along +x to x = 8, then along -z, turning without handles. */
    private static NavigableMap<BlockVector3, BlockVector3> turnLeft() throws Exception {
        CurveLayout.Selection road = new CurveLayout.Selection(1, 1, 5, ORIGIN);
        BezierSegment first = straight(Vector3.at(0, 64, 0), Vector3.at(8, 64, 0));
        BezierSegment second = straight(Vector3.at(8, 64, 0), Vector3.at(8, 64, -8));
        return layout(road, new BcArguments(0, false, BcOptions.Axis.X), 0, first, second).blocks();
    }

    @Test
    void curvePassingOverItselfKeepsTheHigherRowOfEachPart() throws Exception {
        // Along x at y = 64, up four blocks, then back along x at y = 68, with a selection six blocks high: where the
        // two parts share blocks, the top rows of the lower part are laid before the bottom rows of the upper part
        CurveLayout.Selection tall = new CurveLayout.Selection(1, 6, 1, ORIGIN);
        NavigableMap<BlockVector3, BlockVector3> blocks = layout(tall, NO_ARGUMENTS, 0,
                straight(Vector3.at(0, 64, 0), Vector3.at(10, 64, 0)),
                straight(Vector3.at(10, 64, 0), Vector3.at(10, 68, 0)),
                straight(Vector3.at(10, 68, 0), Vector3.at(0, 68, 0))).blocks();

        for (int x = 2; x <= 8; x++) {
            assertEquals(4, blocks.get(BlockVector3.at(x, 68, 0)).y(), String.valueOf(x));
            assertEquals(5, blocks.get(BlockVector3.at(x, 69, 0)).y(), String.valueOf(x));
            assertEquals(2, blocks.get(BlockVector3.at(x, 70, 0)).y(), String.valueOf(x));
            assertEquals(3, blocks.get(BlockVector3.at(x, 67, 0)).y(), String.valueOf(x));
        }
    }

    @Test
    void upperRowKeepsTheBlocksItSharesWithTheRowBelow() throws Exception {
        // Up one block for every two: in each odd column the curve climbs into the block above halfway, which the
        // bottom row reaches after the top row has
        CurveLayout.Selection twoHigh = new CurveLayout.Selection(1, 2, 1, ORIGIN);
        NavigableMap<BlockVector3, BlockVector3> blocks =
                layout(twoHigh, NO_ARGUMENTS, 0, straight(Vector3.at(0, 64, 0), Vector3.at(12, 70, 0))).blocks();

        for (int x = 1; x <= 11; x += 2) {
            BlockVector3 shared = BlockVector3.at(x, 65 + (x - 1) / 2, 0);
            assertEquals(1, blocks.get(shared).y(), shared.toString());
        }
    }

    @Test
    void limitCountsTheBlocksToPlace() throws Exception {
        // There and back over the same blocks, which the limit counts once
        BezierSegment there = straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0));
        BezierSegment back = straight(Vector3.at(6, 64, 0), Vector3.at(0, 64, 0));
        int blocks = layout(ONE_BLOCK, NO_ARGUMENTS, 0, there, back).blocks().size();

        assertEquals(7, blocks);
        assertEquals(blocks, layout(ONE_BLOCK, NO_ARGUMENTS, blocks, there, back).blocks().size());
        CurveLayout tooMany = layout(ONE_BLOCK, NO_ARGUMENTS, blocks - 1, there, back);
        assertThrows(MaxChangedBlocksException.class, tooMany::blocks);
    }

    @Test
    void segmentLongerThanTheMaximumIsRejected() {
        CurveLayout layout = new CurveLayout(ONE_BLOCK, NO_ARGUMENTS, NEVER_AIR, 0, 5, 10);

        BezierSegment line = straight(Vector3.at(0, 64, 0), Vector3.at(6, 64, 0));
        assertThrows(CurveLayout.TooLongException.class, () -> layout.add(line));
    }

    @Test
    void curveLongerThanTheMaximumIsRejectedByItsWholeLength() throws Exception {
        CurveLayout layout = new CurveLayout(ONE_BLOCK, NO_ARGUMENTS, NEVER_AIR, 0, 3, 10);
        BezierSegment[] segments = segmentsOf(1.4, 3);

        // 1.4 + 1.4 is within the maximum of 3, and 1.4 more goes over it
        layout.add(segments[0]);
        layout.add(segments[1]);
        assertThrows(CurveLayout.TooLongException.class, () -> layout.add(segments[2]));
    }

    @Test
    void lengthAddsTheSegmentsWithoutRoundingThem() throws Exception {
        List<BezierSegment> diagonals = List.of(
                straight(Vector3.at(0, 64, 0), Vector3.at(1, 64, 1)),
                straight(Vector3.at(1, 64, 1), Vector3.at(2, 64, 2)));

        CurveLayout layout = layout(ONE_BLOCK, NO_ARGUMENTS, 0, diagonals.toArray(BezierSegment[]::new));

        assertEquals(2 * Math.sqrt(2), layout.length(), 1e-9);
        assertFalse(layout.blocks().isEmpty());
    }

    @Test
    void lengthReachesTheEndOfEachSegment() throws Exception {
        // 10 steps per block do not make a whole number of steps along 1.95 blocks
        CurveLayout layout = layout(ONE_BLOCK, NO_ARGUMENTS, 0, segmentsOf(1.95, 10));

        assertEquals(19.5, layout.length(), 1e-9);
    }
}
