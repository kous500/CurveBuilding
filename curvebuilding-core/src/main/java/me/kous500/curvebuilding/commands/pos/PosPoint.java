package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.curve.CurvePoint.Part;
import me.kous500.curvebuilding.message.GameMessage;
import me.kous500.curvebuilding.message.Message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.kous500.curvebuilding.CurveBuilding.config;

/**
 * A point of the curve, or one of its handles, as typed in {@code //bcpos}: {@code 2} is point 2, {@code 2f} its front
 * handle and {@code 2b} its back handle. The first point has no front handle.
 *
 * @param n the point number, from 1 to the {@code max-pos-value} of the config
 * @param part the point or one of its handles
 */
public record PosPoint(int n, Part part) {
    private static final Pattern POS = Pattern.compile("([0-9]+)([fb]?)");

    /**
     * Reads a point or one of its handles, such as {@code 2}, {@code 2f} or {@code 2b}.
     *
     * @param text the argument as typed
     * @return the point and the part of it
     * @throws ArgumentException when it is not a point, or the point is out of range
     */
    public static PosPoint parse(String text) throws ArgumentException {
        Matcher matcher = POS.matcher(text);
        if (!matcher.matches()) {
            throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
        }

        int n = number(matcher.group(1), text);
        return switch (matcher.group(2)) {
            case "f" -> {
                if (n == 1) throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
                yield new PosPoint(n, Part.F);
            }
            case "b" -> new PosPoint(n, Part.B);
            default -> new PosPoint(n, Part.POINT);
        };
    }

    /**
     * Reads a point number only, such as {@code 2}.
     *
     * @param text the argument as typed
     * @return the point number
     * @throws ArgumentException when it is not a point number, or it is out of range
     */
    public static int parseNumber(String text) throws ArgumentException {
        if (!text.matches("[0-9]+")) {
            throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
        }
        return number(text, text);
    }

    private static int number(String digits, String text) throws ArgumentException {
        int n;
        try {
            n = Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            throw new ArgumentException(GameMessage.INVALID_INTEGER, text);
        }
        if (n < 1) {
            throw new ArgumentException(GameMessage.INTEGER_TOO_LOW, 1, text);
        }
        if (n > config.maxPosValue) {
            throw new ArgumentException(Message.MAX_POS_VALUE, config.maxPosValue);
        }
        return n;
    }
}
