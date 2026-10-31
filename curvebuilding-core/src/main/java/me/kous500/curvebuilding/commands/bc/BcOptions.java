package me.kous500.curvebuilding.commands.bc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The option letters of {@code //bc}, typed after a {@code -} such as {@code -ax}, and which of them were used. Each
 * option can be used once; {@code x} and {@code z} set one axis, so only one of them.
 */
public final class BcOptions {
    /** The axis to read the selection along. */
    public enum Axis {
        X, Z;

        public final String letter = name().toLowerCase(Locale.ROOT);
    }

    /** Replace only air blocks. */
    public static final String AIR = "a";

    private boolean air;
    private Axis axis;

    /**
     * Uses the option {@code letters}, typed after a {@code -}, up to the first letter that is not an option or was
     * already used.
     *
     * @return whether every letter could be used
     */
    public boolean read(String letters) {
        for (char letter : letters.toCharArray()) {
            Axis letterAxis = axisOf(String.valueOf(letter));
            if (String.valueOf(letter).equals(AIR) && !air) {
                air = true;
            } else if (letterAxis != null && axis == null) {
                axis = letterAxis;
            } else {
                return false;
            }
        }
        return true;
    }

    public boolean air() {
        return air;
    }

    /** The axis set, or null to choose it from the selection. */
    public Axis axis() {
        return axis;
    }

    /** The option letters that can still be used, in alphabetical order. */
    public List<String> unusedLetters() {
        List<String> letters = new ArrayList<>();
        if (!air) letters.add(AIR);
        if (axis == null) {
            for (Axis each : Axis.values()) letters.add(each.letter);
        }
        return letters;
    }

    private static Axis axisOf(String letter) {
        for (Axis each : Axis.values()) {
            if (each.letter.equals(letter)) return each;
        }
        return null;
    }
}
