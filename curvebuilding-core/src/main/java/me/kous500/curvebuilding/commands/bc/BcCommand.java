package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.curve.PosData;
import me.kous500.curvebuilding.curve.PosManager;

/**
 * Runs {@code //bc} on every platform.
 */
public final class BcCommand {
    private BcCommand() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Builds the curve with {@code args} as the arguments of {@code //bc}, or shows the error of the first argument
     * that cannot be used. The curve is built before this returns.
     *
     * @param player the player who ran the command
     * @param args the arguments of the command
     * @param config the config in use
     * @return whether the arguments could be used
     */
    public static boolean bcCommand(Player player, String[] args, Config config) {
        return bcCommand(player, args, config, EditRunner.DIRECT);
    }

    /**
     * Has {@code runner} build the curve with {@code args} as the arguments of {@code //bc}, or shows the error of the
     * first argument that cannot be used without calling it. The curve and {@code config} are those at the time of the
     * call, and the selection is read when {@code runner} runs the edit.
     *
     * @param player the player who ran the command
     * @param args the arguments of the command
     * @param config the config in use
     * @param runner runs the edit
     * @return whether the arguments could be used
     */
    public static boolean bcCommand(Player player, String[] args, Config config, EditRunner runner) {
        BcArguments arguments;
        try {
            arguments = BcArguments.parse(args);
        } catch (ArgumentException e) {
            e.show(player, "bc", args);
            return false;
        }
        PosData posData = PosManager.get(player);
        PosData curve = posData == null ? null : posData.copy();
        runner.run(player, started -> BcEdit.build(player, arguments, config, curve, started));
        return true;
    }
}
