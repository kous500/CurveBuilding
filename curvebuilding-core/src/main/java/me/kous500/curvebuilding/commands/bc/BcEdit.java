package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.config.Message;
import me.kous500.curvebuilding.math.BezierSegment;
import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.math.PosManager;

import java.util.Map;

import static me.kous500.curvebuilding.CurveBuilding.config;
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
     * Builds the curve through the positions of {@code player} with the blocks of their selection, as one change that
     * {@code //undo} takes back.
     */
    public static void build(Player player, BcArguments arguments) {
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
            Region region = session.getSelection(world);

            PosData posData = PosManager.get(player);
            if (posData == null || posData.points().get(1) == null || posData.points().get(1).point() == null) {
                throw new IncompletePosException();
            }
            if (!posData.isIn(world)) throw new IncompleteRegionException();

            if (0 < maxBlocks && maxBlocks < region.getVolume()) {
                throw new MaxChangedBlocksException(maxBlocks);
            }

            RegionBlocks regionBlocks = new RegionBlocks(editSession, region);
            CurveLayout.Selection selection = new CurveLayout.Selection(
                    region.getWidth(), region.getHeight(), region.getLength(), adapt(region.getCenter()));
            CurveLayout layout = new CurveLayout(selection, arguments,
                    position -> editSession.getBlock(adapt(position)).getBlockType().getMaterial().isAir(),
                    maxBlocks, config.maxSetLength, config.fineness);
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
