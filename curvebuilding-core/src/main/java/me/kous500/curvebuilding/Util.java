package me.kous500.curvebuilding;

import static java.lang.Math.sqrt;

/** Small calculations shared by the rest of the code. */
public final class Util {

    private Util() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * The vector (x, y, z) scaled to the length {@code newR}, keeping its direction; a negative length turns it the
     * other way. A zero vector stays zero.
     *
     * @param x the x component
     * @param y the y component
     * @param z the z component
     * @param newR the length to scale to
     * @return the scaled vector, as {x, y, z}
     */
    public static double[] changeR(double x, double y, double z, double newR) {
        double currentR = sqrt(x * x + y * y + z * z);

        if (currentR == 0) {
            return new double[]{0, 0, 0};
        }

        double scale = newR / currentR;
        return new double[]{ x * scale, y * scale, z * scale };
    }

    /**
     * {@code message} with {@code {0}}, {@code {1}}, ... replaced with {@code args} in that order. A placeholder
     * without an argument stays as it is.
     *
     * @param message the text, or null
     * @param args the values for the placeholders, or null
     * @return the text with the values, or null when {@code message} is null
     */
    public static String messageReplace(String message, Object... args) {
        if (message == null || args == null) {
            return message;
        }

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return message;
    }
}
