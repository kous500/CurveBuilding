package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.curve.BezierSegment;
import me.kous500.curvebuilding.curve.PosData;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.message.Chat;
import me.kous500.curvebuilding.message.Message;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

import static me.kous500.curvebuilding.WorldeditAdapter.adapt;

/** Builds the curve of {@code //bc} in the world, with WorldEdit, and replies to the player. */
public final class BcEdit {
    private BcEdit() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /** The positions of the player do not start with point 1. */
    private static final class IncompletePosException extends Exception {
        IncompletePosException() {
            super(null, null, false, false);
        }
    }

    /**
     * Builds the curve of {@code posData} with the blocks of the selection of {@code player}, as one change that
     * {@code //undo} takes back.
     *
     * @param player the player who ran the command, whose selection is used
     * @param arguments the arguments of {@code //bc}
     * @param config the config in use, for the limits
     * @param posData the curve, which must not change while it is built, or null without one
     * @param started given the edit session before anything is read or placed with it
     */
    public static void build(
            Player player,
            BcArguments arguments,
            Config config,
            @Nullable PosData posData,
            Consumer<EditSession> started) {
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
        World world = session.getSelectionWorld();

        // The lower of the limit of the player and that of the config; the limit of the player stays as it is
        int maxBlocks = session.getBlockChangeLimit();
        int configLimit = config.defaultMaxChangeLimit;
        if (configLimit >= 0 && (maxBlocks < 0 || maxBlocks > configLimit)) {
            maxBlocks = configLimit;
        }

        EditSession editSession = session.createEditSession(player);
        try (editSession) {
            started.accept(editSession);
            if (world == null) throw new IncompleteRegionException();
            Region region = session.getSelection(world);

            if (posData == null || posData.points().get(1) == null || posData.points().get(1).point() == null) {
                throw new IncompletePosException();
            }
            if (!posData.isIn(world)) throw new IncompleteRegionException();

            // Every block of the selection is read before the curve is built; a huge selection would freeze the
            // server, so the limit applies to it too
            if (0 < maxBlocks && maxBlocks < region.getVolume()) {
                throw new MaxChangedBlocksException(maxBlocks);
            }

            RegionBlocks regionBlocks = new RegionBlocks(editSession, region);
            CurveLayout.Selection selection = new CurveLayout.Selection(
                    region.getWidth(),
                    region.getHeight(),
                    region.getLength(),
                    adapt(region.getMinimumPoint())
            );
            CurveLayout layout = new CurveLayout(
                    selection,
                    arguments,
                    position -> editSession.getBlock(adapt(position)).getBlockType().getMaterial().isAir(),
                    maxBlocks,
                    config.maxSetLength
            );
            for (BezierSegment segment : BezierSegment.through(posData.points())) {
                layout.add(segment);
            }

            for (Map.Entry<BlockVector3, BlockVector3> block : layout.blocks().entrySet()) {
                editSession.setBlock(adapt(block.getKey()), regionBlocks.get(block.getValue()));
            }

            Chat.info(player, Message.BC_CHANGED, editSession.size());
            Chat.info(player, Message.BC_LENGTH, Math.round(layout.length() * 100) / 100.0);
        } catch (IncompleteRegionException e) {
            Chat.error(player, Message.INCOMPLETE_REGION);
        } catch (IncompletePosException e) {
            Chat.error(player, Message.INCOMPLETE_POS);
        } catch (MaxChangedBlocksException e) {
            Chat.error(player, Message.MAX_CHANGED_BLOCKS, maxBlocks);
        } catch (CurveLayout.TooLongException e) {
            Chat.error(player, Message.MAX_SET_LENGTH, config.maxSetLength);
        } finally {
            session.remember(editSession);
        }
    }
}
