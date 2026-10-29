package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.math.PosManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestions for the arguments of {@code //bcpos}, for the Bukkit tab completion and the Fabric command suggestions.
 */
public final class PosCompletion {
    private PosCompletion() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Suggestions for the last of {@code args}, the argument being typed, sorted. After a length, {@code shift}
     * suggests the length with each direction (for example {@code 5 back}), so both are filled in at once.
     *
     * @param args the arguments typed so far; the last one may be empty
     */
    public static List<String> complete(String[] args) {
        if (args.length == 0) return List.of();

        List<String> candidates = new ArrayList<>();
        String typed = args[args.length - 1];
        if (args.length == 1) {
            for (PosSubcommand subcommand : PosSubcommand.values()) candidates.add(subcommand.label);
        } else if (args.length == 2) {
            PosSubcommand subcommand = PosSubcommand.of(args[0]);
            if (subcommand != null) {
                switch (subcommand.arguments) {
                    case POS_TYPE -> addPositions(typed, candidates, new String[]{"f", "b"});
                    case POINT -> addPositions(typed, candidates, new String[0]);
                    case LENGTH_AND_DIRECTION -> {
                        if (typed.matches("[0-9]+")) {
                            for (String direction : PosManager.directionOptions) candidates.add(typed + " " + direction);
                        }
                    }
                    case NONE -> { }
                }
            }
        }
        return startingWith(typed, candidates);
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
                for (int digit = 0; digit <= 9; digit++) candidates.add(typed + digit);
            }
        } else if (typed.isEmpty()) {
            for (int n = 1; n <= 9; n++) candidates.add(String.valueOf(n));
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
