package me.kous500.curvebuilding.commands.pos;

import me.kous500.curvebuilding.GameMessage;
import me.kous500.curvebuilding.commands.ArgumentException;
import me.kous500.curvebuilding.math.Vector3;

import java.util.Locale;

/** The directions of {@code //bcpos shift}. Their first letters differ, so each can also be typed as its first letter. */
public enum Direction {
    BACK, DOWN, EAST, FORWARD, LEFT, ME, NORTH, RIGHT, SOUTH, UP, WEST;

    /** The name to type, such as {@code back}. */
    public final String label = name().toLowerCase(Locale.ROOT);

    /** The direction typed as its name or its first letter, ignoring case. */
    public static Direction parse(String text) throws ArgumentException {
        String typed = text.toLowerCase(Locale.ROOT);
        for (Direction direction : values()) {
            if (direction.label.equals(typed) || direction.label.substring(0, 1).equals(typed)) return direction;
        }
        throw new ArgumentException(GameMessage.INCORRECT_ARGUMENT);
    }

    /**
     * The offset of {@code length} blocks in this direction, for a player looking at {@code yaw} and {@code pitch} in
     * degrees as Minecraft gives them. {@code me} goes up or down when the player looks more than 67.5 degrees up or
     * down, and forward otherwise. The directions that follow the yaw go along each axis within 67.5 degrees of it, so
     * along two axes at once near a diagonal.
     */
    public Vector3 offset(int length, double yaw, double pitch) {
        switch (this) {
            case RIGHT -> yaw += 90;
            case LEFT -> yaw -= 90;
            case BACK -> yaw += 180;
            default -> { }
        }
        if (yaw < -180) yaw += 360;
        else if (yaw > 180) yaw -= 360;

        if (this == DOWN || this == ME && pitch > 67.5) return Vector3.at(0, -length, 0);
        if (this == UP || this == ME && pitch < -67.5) return Vector3.at(0, length, 0);

        boolean byYaw = this != NORTH && this != SOUTH && this != EAST && this != WEST;
        Vector3 offset = Vector3.at(0, 0, 0);
        if (byYaw && Math.abs(yaw) < 67.5 || this == SOUTH) offset = offset.add(0, 0, length);
        if (byYaw && 180 - Math.abs(yaw) < 67.5 || this == NORTH) offset = offset.add(0, 0, -length);
        if (byYaw && Math.abs(yaw + 90) < 67.5 || this == EAST) offset = offset.add(length, 0, 0);
        if (byYaw && Math.abs(yaw - 90) < 67.5 || this == WEST) offset = offset.add(-length, 0, 0);
        return offset;
    }
}
