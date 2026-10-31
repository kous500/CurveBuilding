package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;

/**
 * The subcommands of {@code //bcpos} and the arguments each takes. The command and its suggestions on every
 * platform are built from this list.
 */
public enum PosSubcommand {
    CLEAR("clear", Arguments.POS_TYPE),
    CLEARALL("clearall", Arguments.NONE),
    INSERT("insert", Arguments.POINT),
    REMOVE("remove", Arguments.POINT),
    SET("set", Arguments.POS_TYPE),
    SHIFT("shift", Arguments.LENGTH_AND_DIRECTION);

    public enum Arguments {
        NONE(0),
        /** A point or one of its handles, such as {@code 2}, {@code 2f} or {@code 2b} (see {@link PosPoint}). */
        POS_TYPE(1),
        /** A point number only, such as {@code 2}. */
        POINT(1),
        /** A length, then an optional {@link Direction}. */
        LENGTH_AND_DIRECTION(2);

        /** The most arguments after the subcommand. */
        public final int max;

        Arguments(int max) {
            this.max = max;
        }
    }

    public final String label;
    public final Arguments arguments;

    PosSubcommand(String label, Arguments arguments) {
        this.label = label;
        this.arguments = arguments;
    }

    /** The subcommand typed as {@code label}, or null. */
    public static PosSubcommand of(String label) {
        for (PosSubcommand subcommand : values()) {
            if (subcommand.label.equals(label)) return subcommand;
        }
        return null;
    }

    public static PosSubcommand parse(String label) throws ArgumentException {
        PosSubcommand subcommand = of(label);
        if (subcommand == null) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
        return subcommand;
    }
}
