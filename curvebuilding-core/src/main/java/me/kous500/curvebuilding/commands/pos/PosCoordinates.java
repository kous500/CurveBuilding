package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.message.GameMessage;

/**
 * The x, y and z of a position in {@code //bcpos}, read as Minecraft reads a position in its commands, such as those of
 * {@code /tp}. {@code 100.5} is the coordinate itself. {@code ~5} is 5 blocks from the player on that axis. {@code ^1
 * ^2 ^3} is 1 block to the left, 2 up and 3 forward from where the player looks, and all three use {@code ^} then.
 * Unlike in Minecraft, {@code ~} and {@code ^} count from half a block above the feet of the player: from the middle
 * of a block they stand on, {@code ~ ~ ~} is the center of the block their feet are in, where {@code //bcpos} puts a
 * point without coordinates.
 */
public final class PosCoordinates {
    // Minecraft's sine table has 65536 steps per turn; this is the number of steps per radian
    private static final double SINE_STEPS_PER_RADIAN = 10430.378350470453;

    private final boolean local;
    private final boolean[] relative;
    private final double[] values;

    private PosCoordinates(boolean local, boolean[] relative, double[] values) {
        this.local = local;
        this.relative = relative;
        this.values = values;
    }

    /**
     * Reads the three coordinates from {@code args[start]} on.
     *
     * @param args the arguments of the command
     * @param start the index of the x coordinate
     * @return the coordinates
     * @throws ArgumentException when there are fewer than three, one cannot be read, or {@code ^} is mixed with the
     * others; it is the error of that argument
     */
    public static PosCoordinates parse(String[] args, int start) throws ArgumentException {
        if (args.length < start + 3) {
            throw new ArgumentException(GameMessage.INCOMPLETE_POSITION).at(args.length);
        }

        boolean local = args[start].startsWith("^");
        boolean[] relative = new boolean[3];
        double[] values = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            String text = args[start + axis];
            try {
                if (text.startsWith("^") != local) throw new ArgumentException(GameMessage.MIXED_COORDINATES);
                relative[axis] = local || text.startsWith("~");
                values[axis] = relative[axis] ? number(text.substring(1)) : number(text);
            } catch (ArgumentException e) {
                throw e.at(start + axis);
            }
        }
        return new PosCoordinates(local, relative, values);
    }

    /**
     * A coordinate, or the number after {@code ~} or {@code ^}: 0 when there is none, and of the characters that
     * Minecraft reads.
     */
    private static double number(String text) throws ArgumentException {
        if (text.isEmpty()) return 0;
        if (text.matches("[0-9.-]+")) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                // Falls through to the error below, as for "1.2.3"
            }
        }
        throw new ArgumentException(GameMessage.INVALID_DOUBLE, text);
    }

    /**
     * The position at these coordinates, not rounded, for a player at {@code feet} looking at {@code yaw} and
     * {@code pitch}. For {@code ^}, the directions are computed as in Minecraft 26.3, with its table of sines.
     *
     * @param feet where the player is: the position of their feet
     * @param yaw where the player looks to the sides, in degrees as Minecraft gives it: 0 is south, 90 west
     * @param pitch where the player looks up or down, in degrees: positive is down
     * @return the position
     */
    public Vector3 position(Vector3 feet, float yaw, float pitch) {
        Vector3 base = feet.add(0, 0.5, 0);
        if (local) return local(base, yaw, pitch);

        double x = relative[0] ? base.x() + values[0] : values[0];
        double y = relative[1] ? base.y() + values[1] : values[1];
        double z = relative[2] ? base.z() + values[2] : values[2];
        return Vector3.at(x, y, z);
    }

    private Vector3 local(Vector3 position, float yaw, float pitch) {
        float yawCos = cos((yaw + 90.0F) * 0.017453292F);
        float yawSin = sin((yaw + 90.0F) * 0.017453292F);
        float pitchCos = cos(-pitch * 0.017453292F);
        float pitchSin = sin(-pitch * 0.017453292F);
        float upCos = cos((-pitch + 90.0F) * 0.017453292F);
        float upSin = sin((-pitch + 90.0F) * 0.017453292F);

        Vector3 forward = Vector3.at(yawCos * pitchCos, pitchSin, yawSin * pitchCos);
        Vector3 up = Vector3.at(yawCos * upCos, upSin, yawSin * upCos);
        Vector3 left = Vector3.at(
                forward.y() * up.z() - forward.z() * up.y(),
                forward.z() * up.x() - forward.x() * up.z(),
                forward.x() * up.y() - forward.y() * up.x()
        ).multiply(-1);

        double leftBlocks = values[0];
        double upBlocks = values[1];
        double forwardBlocks = values[2];
        return position.add(
                forward.x() * forwardBlocks + up.x() * upBlocks + left.x() * leftBlocks,
                forward.y() * forwardBlocks + up.y() * upBlocks + left.y() * leftBlocks,
                forward.z() * forwardBlocks + up.z() * upBlocks + left.z() * leftBlocks
        );
    }

    /** {@code Mth.sin} of Minecraft 26.3: the sine from a table of 65536 steps per turn. */
    private static float sin(double radians) {
        return (float) Math.sin((int) ((long) (radians * SINE_STEPS_PER_RADIAN) & 65535L) / SINE_STEPS_PER_RADIAN);
    }

    /** {@code Mth.cos} of Minecraft 26.3, from the same table, a quarter turn further. */
    private static float cos(double radians) {
        return (float) Math.sin(
                (int) ((long) (radians * SINE_STEPS_PER_RADIAN + 16384.0) & 65535L) / SINE_STEPS_PER_RADIAN);
    }
}
