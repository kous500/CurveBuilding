package me.kous500.curvebuilding.commands.bc;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggestions for the options of {@code //bc}, for the command suggestions of every platform.
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
        BcOptions options = new BcOptions();
        List<String> completions = List.of();

        for (String arg : args) {
            List<String> candidates = new ArrayList<>();
            if (arg.startsWith("-") && options.read(arg.substring(1))) {
                for (String letter : options.unusedLetters()) candidates.add(arg + letter);
            }
            completions = startingWith(arg, candidates);
        }
        return completions;
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
