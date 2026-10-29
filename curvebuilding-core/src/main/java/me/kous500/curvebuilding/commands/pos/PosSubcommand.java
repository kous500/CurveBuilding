package me.kous500.curvebuilding.commands.pos;

/**
 * The subcommands of {@code //bcpos} and the arguments each takes. The command, its tab completion on Bukkit and its
 * command tree on Fabric are all built from this list.
 */
public enum PosSubcommand {
    CLEAR("clear", Arguments.POS_TYPE),
    CLEARALL("clearall", Arguments.NONE),
    INSERT("insert", Arguments.POINT),
    REMOVE("remove", Arguments.POINT),
    SET("set", Arguments.POS_TYPE),
    SHIFT("shift", Arguments.LENGTH_AND_DIRECTION);

    public enum Arguments {
        NONE,
        /** A point or one of its handles, such as {@code 2}, {@code 2f} or {@code 2b} (see {@link PosType}). */
        POS_TYPE,
        /** A point number only, such as {@code 2}. */
        POINT,
        /** A length, then an optional direction from {@code PosManager.directionOptions}. */
        LENGTH_AND_DIRECTION
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
}
