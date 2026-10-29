package me.kous500.curvebuilding.commands.bc;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestions for the options of {@code //bc}, for the Bukkit tab completion and the Fabric command suggestions.
 */
public final class BcCompletion {
    private BcCompletion() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Suggestions for the last of {@code args}, the argument being typed, sorted. An argument starting with {@code -}
     * gets each option letter that no earlier argument used, added to its end; any other argument gets none.
     *
     * @param args the arguments typed so far; the last one may be empty
     */
    public static List<String> complete(String[] args) {
        boolean airUsed = false;
        boolean directionUsed = false;
        List<String> completions = List.of();

        for (String arg : args) {
            List<String> candidates = new ArrayList<>();
            if (arg.startsWith("-")) {
                boolean valid = true;
                // The first character is the "-"
                for (String letter : arg.substring(1).split("")) {
                    if (letter.isEmpty()) continue;
                    if (letter.equals(BcCommand.AIR_OPTION) && !airUsed && valid) {
                        airUsed = true;
                    } else if (isDirection(letter) && !directionUsed && valid) {
                        directionUsed = true;
                    } else {
                        valid = false;
                    }
                }

                if (valid) {
                    if (!airUsed) candidates.add(arg + BcCommand.AIR_OPTION);
                    if (!directionUsed) {
                        for (String direction : BcCommand.DIRECTION_OPTIONS) candidates.add(arg + direction);
                    }
                }
            }
            completions = startingWith(arg, candidates);
        }
        return completions;
    }

    private static boolean isDirection(String letter) {
        for (String direction : BcCommand.DIRECTION_OPTIONS) {
            if (direction.equals(letter)) return true;
        }
        return false;
    }

    private static List<String> startingWith(String typed, List<String> candidates) {
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.regionMatches(true, 0, typed, 0, typed.length())) matches.add(candidate);
        }
        matches.sort(null);
        return matches;
    }
}
