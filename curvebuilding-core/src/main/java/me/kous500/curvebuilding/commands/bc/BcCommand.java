package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.config.Config;

/**
 * Runs {@code //bc} on every platform.
 */
public final class BcCommand {
    private BcCommand() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Builds the curve with {@code args} as the arguments of {@code //bc}, or shows the error of the first argument
     * that cannot be used.
     *
     * @param player the player who ran the command
     * @param args the arguments of the command
     * @param config the config in use
     * @return whether the arguments could be used
     */
    public static boolean bcCommand(Player player, String[] args, Config config) {
        BcArguments arguments;
        try {
            arguments = BcArguments.parse(args);
        } catch (ArgumentException e) {
            e.show(player, "bc", args);
            return false;
        }
        BcEdit.build(player, arguments, config);
        return true;
    }
}
