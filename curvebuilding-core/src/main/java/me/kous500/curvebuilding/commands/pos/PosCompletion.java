package me.kous500.curvebuilding.commands.pos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Suggestions for the arguments of {@code //bcpos}, for the command suggestions of every platform.
 */
public final class PosCompletion {
    private PosCompletion() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Suggestions for the last of {@code args}, the argument being typed, sorted. After a length, {@code shift}
     * suggests the length with each direction (for example {@code 5 back}), so both are filled in at once. The
     * coordinates of {@code set} and {@code insert} are suggested the same way (see {@link #addCoordinates}).
     *
     * @param args the arguments typed so far; the last one may be empty
     * @return the suggestions, or an empty list
     */
    public static List<String> complete(String[] args) {
        if (args.length == 0) return List.of();

        List<String> candidates = new ArrayList<>();
        String typed = args[args.length - 1];
        if (args.length == 1) {
            for (PosSubcommand subcommand : PosSubcommand.values()) {
                candidates.add(subcommand.label);
            }
        } else if (args.length == 2) {
            PosSubcommand subcommand = PosSubcommand.of(args[0]);
            if (subcommand != null) {
                switch (subcommand.arguments) {
                    case POS_TYPE, POS_TYPE_AND_POSITION -> addPositions(typed, candidates, new String[]{"f", "b"});
                    case POINT, POINT_AND_POSITION -> addPositions(typed, candidates, new String[0]);
                    case LENGTH_AND_DIRECTION -> {
                        if (typed.matches("[0-9]+")) {
                            for (Direction direction : Direction.values()) {
                                candidates.add(typed + " " + direction.label);
                            }
                        }
                    }
                    case NONE -> { }
                }
            }
        } else if (args.length <= 5) {
            PosSubcommand subcommand = PosSubcommand.of(args[0]);
            if (subcommand != null && (subcommand.arguments == PosSubcommand.Arguments.POS_TYPE_AND_POSITION
                    || subcommand.arguments == PosSubcommand.Arguments.POINT_AND_POSITION)) {
                addCoordinates(args, candidates);
            }
        }
        return startingWith(typed, candidates);
    }

    /**
     * The coordinate being typed, with the ones after it. Before one is typed, or after {@code ~} alone, {@code ~},
     * {@code ~ ~} and so on up to the last coordinate. After a number, also one after {@code ~}, it with each number of
     * {@code ~} after it (for example {@code 100 ~} and {@code 100 ~ ~}). When the first coordinate uses {@code ^}, the
     * coordinate with all the rest as {@code ^} (for example {@code ^5 ^ ^}), as {@code ^} cannot be mixed with the
     * others; {@code ^} is not suggested otherwise.
     */
    private static void addCoordinates(String[] args, List<String> candidates) {
        String typed = args[args.length - 1];
        int after = 5 - args.length;
        boolean local = args.length == 3 ? typed.startsWith("^") : args[2].startsWith("^");
        if (local) {
            if (typed.isEmpty() || typed.startsWith("^")) {
                String first = typed.isEmpty() ? "^" : typed;
                if (typed.isEmpty() || after > 0) candidates.add(first + " ^".repeat(after));
            }
        } else if (typed.isEmpty() || typed.equals("~")) {
            for (int count = 1; count <= after + 1; count++) {
                candidates.add(String.join(" ", Collections.nCopies(count, "~")));
            }
        } else if (typed.matches("~?[0-9.-]+")) {
            for (int count = 1; count <= after; count++) {
                candidates.add(typed + " ~".repeat(count));
            }
        }
    }

    /**
     * Positions 1 to 9 for an empty argument. After a number, the number with each handle suffix (the first point has
     * no front handle), or with one more digit when there are no suffixes.
     */
    private static void addPositions(String typed, List<String> candidates, String[] suffixes) {
        if (typed.matches("[1-9][0-9]*")) {
            for (String suffix : suffixes) {
                if (!(typed + suffix).equals("1f")) candidates.add(typed + suffix);
            }
            if (suffixes.length == 0) {
                for (int digit = 0; digit <= 9; digit++) {
                    candidates.add(typed + digit);
                }
            }
        } else if (typed.isEmpty()) {
            for (int n = 1; n <= 9; n++) {
                candidates.add(String.valueOf(n));
            }
        }
    }

    /** The candidates that start with {@code typed}, ignoring case, sorted. */
    static List<String> startingWith(String typed, List<String> candidates) {
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.regionMatches(true, 0, typed, 0, typed.length())) matches.add(candidate);
        }
        matches.sort(null);
        return matches;
    }
}
