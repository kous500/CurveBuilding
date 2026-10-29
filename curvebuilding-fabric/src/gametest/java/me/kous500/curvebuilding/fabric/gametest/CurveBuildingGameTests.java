package me.kous500.curvebuilding.fabric.gametest;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestion;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.permission.ActorSelectorLimits;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.RegionSelector;
import me.kous500.curvebuilding.commands.bc.BcCommand;
import me.kous500.curvebuilding.commands.bc.BcEdit;
import me.kous500.curvebuilding.fabric.CurveBuildingFabric;
import me.kous500.curvebuilding.fabric.FabricCompat;
import me.kous500.curvebuilding.math.PosManager;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests on a real server with WorldEdit, run with runGameTest. Each test gets an empty 8x8x8 area, and positions in
 * the tests are relative to it. The curve tests call what {@code //bcpos} and {@code //bc} call, with a mock player.
 */
public class CurveBuildingGameTests {
    private static final int SIZE = 8;

    @GameTest
    public void defaultFilesAreCopied(GameTestHelper helper) {
        // The files are copied from the mod jar at startup; this failed under a launcher in 0.6.5 - 0.6.8 (#64)
        Path folder = Path.of(CurveBuildingFabric.MOD_CONFIG_PASS);
        for (String file : new String[]{"config.yml", "messages/en-US.yml", "messages/ja-JP.yml"}) {
            helper.assertTrue(Files.isRegularFile(folder.resolve(file)), Component.literal(file + " was not copied to " + folder));
        }
        helper.succeed();
    }

    @GameTest
    public void commandsAreRegistered(GameTestHelper helper) {
        var root = helper.getLevel().getServer().getCommands().getDispatcher().getRoot();
        for (String command : new String[]{"/bc", "/bcpos"}) {
            helper.assertTrue(root.getChild(command) != null, Component.literal("/" + command + " is not registered"));
        }
        helper.succeed();
    }

    @GameTest
    public void commandsSuggestLikeBukkit(GameTestHelper helper) {
        assertSuggestions(helper, "//bcpos s", List.of("set", "shift"));
        assertSuggestions(helper, "//bcpos set 3", List.of("3b", "3f"));
        assertSuggestions(helper, "//bcpos insert ", List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"));
        assertSuggestions(helper, "//bcpos shift 5 ", List.of());
        assertSuggestions(helper, "//bc -a", List.of("-ax", "-az"));

        // The length and a direction are filled in at once, and the result is a command that parses to the end
        String command = "//bcpos shift 5";
        Suggestion back = suggestions(helper, command).stream()
                .filter(suggestion -> suggestion.getText().equals("5 back")).findFirst().orElse(null);
        helper.assertTrue(back != null, Component.literal("no \"5 back\" for " + command));
        String applied = back.apply(command);
        helper.assertTrue(applied.equals("//bcpos shift 5 back"), Component.literal("\"5 back\" gave " + applied));
        helper.assertTrue(!parse(helper, applied).getReader().canRead(), Component.literal(applied + " does not parse"));
        helper.succeed();
    }

    @GameTest
    public void straightCurveCopiesTheSelection(GameTestHelper helper) {
        Curve curve = new Curve(helper);
        curve.select(Blocks.STONE, new BlockPos(0, 1, 0));
        curve.pos(1, 0, new BlockPos(1, 1, 3));
        curve.pos(2, 0, new BlockPos(6, 1, 3));
        curve.build();

        // The selection itself, then one block at each position from the start to the end
        assertBlocks(helper, Blocks.STONE, List.of(
                new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 3), new BlockPos(2, 1, 3), new BlockPos(3, 1, 3),
                new BlockPos(4, 1, 3), new BlockPos(5, 1, 3), new BlockPos(6, 1, 3)
        ));
        helper.succeed();
    }

    @GameTest
    public void bentCurveFollowsTheControlPoints(GameTestHelper helper) {
        Curve curve = new Curve(helper);
        curve.select(Blocks.STONE, new BlockPos(0, 1, 0));
        curve.pos(1, 0, new BlockPos(1, 1, 1));
        curve.pos(1, 2, new BlockPos(6, 1, 1));
        curve.pos(2, 0, new BlockPos(6, 1, 6));
        curve.pos(2, 1, new BlockPos(6, 1, 1));
        curve.build();

        // A quarter-circle-like arc from (1, 1, 1) to (6, 1, 6), bending towards the shared control point (6, 1, 1)
        assertBlocks(helper, Blocks.STONE, List.of(
                new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 1), new BlockPos(2, 1, 1), new BlockPos(3, 1, 1), new BlockPos(4, 1, 1),
                new BlockPos(5, 1, 1), new BlockPos(5, 1, 2), new BlockPos(6, 1, 2), new BlockPos(6, 1, 3),
                new BlockPos(6, 1, 4), new BlockPos(6, 1, 5), new BlockPos(6, 1, 6)
        ));
        helper.succeed();
    }

    @GameTest
    public void segmentBetweenPositionsAtTheSamePlaceIsSkipped(GameTestHelper helper) {
        Curve curve = new Curve(helper);
        curve.select(Blocks.STONE, new BlockPos(0, 1, 0));
        curve.pos(1, 0, new BlockPos(1, 1, 3));
        curve.pos(2, 0, new BlockPos(1, 1, 3));
        curve.pos(3, 0, new BlockPos(6, 1, 3));
        curve.build();

        // Nothing from position 1 to 2, then the same blocks as a straight curve from 2 to 3
        assertBlocks(helper, Blocks.STONE, List.of(
                new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 3), new BlockPos(2, 1, 3), new BlockPos(3, 1, 3),
                new BlockPos(4, 1, 3), new BlockPos(5, 1, 3), new BlockPos(6, 1, 3)
        ));
        helper.succeed();
    }

    @GameTest
    public void loopWithItsEndsAtTheSamePlaceIsBuilt(GameTestHelper helper) {
        Curve curve = new Curve(helper);
        curve.select(Blocks.STONE, new BlockPos(0, 1, 0));
        curve.pos(1, 0, new BlockPos(1, 1, 1));
        curve.pos(1, 2, new BlockPos(1, 1, 6));
        curve.pos(2, 0, new BlockPos(1, 1, 1));
        curve.pos(2, 1, new BlockPos(6, 1, 1));
        curve.build();

        // The loop goes out along the control points and back, so it places more than its two ends
        List<BlockPos> stone = blocks(helper, Blocks.STONE);
        helper.assertTrue(stone.contains(new BlockPos(1, 1, 1)) && stone.size() > 4,
                Component.literal("expected a loop from (1, 1, 1), found stone at " + stone));
        helper.succeed();
    }

    @GameTest
    public void curveLongerThanTheLimitIsNotBuilt(GameTestHelper helper) {
        Curve curve = new Curve(helper);
        curve.select(Blocks.STONE, new BlockPos(0, 1, 0));
        curve.pos(1, 0, new BlockPos(1, 1, 3));
        // Beyond curve.max-set-length (10000 by default)
        curve.pos(2, 0, new BlockPos(1 + 20000, 1, 3));
        curve.build();

        assertBlocks(helper, Blocks.STONE, List.of(new BlockPos(0, 1, 0)));
        helper.succeed();
    }

    @GameTest
    public void builtCurveCanBeUndone(GameTestHelper helper) {
        Curve curve = new Curve(helper);
        curve.select(Blocks.STONE, new BlockPos(0, 1, 0));
        curve.pos(1, 0, new BlockPos(1, 1, 3));
        curve.pos(2, 0, new BlockPos(6, 1, 3));
        curve.build();
        curve.undo();

        assertBlocks(helper, Blocks.STONE, List.of(new BlockPos(0, 1, 0)));
        helper.succeed();
    }

    private static void assertBlocks(GameTestHelper helper, Block block, List<BlockPos> expected) {
        List<BlockPos> actual = blocks(helper, block);
        helper.assertTrue(actual.equals(expected), Component.literal("expected " + block + " at " + expected + ", found at " + actual));
    }

    /**
     * Parses {@code command} as the server does for a player's chat input, skipping its first {@code /}, with the
     * permissions of the server console.
     */
    private static ParseResults<CommandSourceStack> parse(GameTestHelper helper, String command) {
        MinecraftServer server = helper.getLevel().getServer();
        StringReader reader = new StringReader(command);
        reader.skip();
        return server.getCommands().getDispatcher().parse(reader, server.createCommandSourceStack());
    }

    /** The suggestions the server sends back for {@code command} typed up to its end. */
    private static List<Suggestion> suggestions(GameTestHelper helper, String command) {
        MinecraftServer server = helper.getLevel().getServer();
        return server.getCommands().getDispatcher().getCompletionSuggestions(parse(helper, command)).join().getList();
    }

    private static void assertSuggestions(GameTestHelper helper, String command, List<String> expected) {
        List<String> found = suggestions(helper, command).stream().map(Suggestion::getText).toList();
        helper.assertTrue(found.equals(expected), Component.literal("expected " + expected + " for " + command + ", found " + found));
    }

    /** Every position in the test area with {@code block}, ordered by x, then y, then z. */
    private static List<BlockPos> blocks(GameTestHelper helper, Block block) {
        List<BlockPos> found = new ArrayList<>();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                for (int z = 0; z < SIZE; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (helper.getBlockState(pos).is(block)) found.add(pos);
                }
            }
        }
        return found;
    }

    /** Sets up and builds a curve for a new mock player, the way the commands do. */
    private static final class Curve {
        private final GameTestHelper helper;
        private final ServerPlayer serverPlayer;
        private final Player player;

        Curve(GameTestHelper helper) {
            this.helper = helper;
            this.serverPlayer = helper.makeMockServerPlayerInLevel();
            this.player = FabricCompat.adaptPlayer(serverPlayer);
        }

        /** Places {@code block} at {@code pos} and selects it with WorldEdit, as //pos1 and //pos2 would. */
        void select(Block block, BlockPos pos) {
            helper.setBlock(pos, block);
            BlockPos absolute = helper.absolutePos(pos);
            BlockVector3 vector = BlockVector3.at(absolute.getX(), absolute.getY(), absolute.getZ());
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            RegionSelector selector = session.getRegionSelector(player.getWorld());
            selector.selectPrimary(vector, ActorSelectorLimits.forActor(player));
            selector.selectSecondary(vector, ActorSelectorLimits.forActor(player));
        }

        /** Sets curve position {@code n} (h: 0 the point, 1 and 2 its control points) where the player stands, as //bcpos does. */
        void pos(int n, int h, BlockPos pos) {
            BlockPos absolute = helper.absolutePos(pos);
            serverPlayer.setPos(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
            PosManager.addPos(player, n, h);
        }

        /** Builds the curve, as //bc without arguments does. */
        void build(String... arguments) {
            BcCommand command = new BcCommand(arguments, player);
            helper.assertTrue(command.success, Component.literal("invalid arguments"));
            new BcEdit(player, command);
        }

        /** Undoes the last change of the player, as //undo does. */
        void undo() {
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
            helper.assertTrue(session.undo(null, player) != null, Component.literal("nothing to undo"));
        }
    }
}
