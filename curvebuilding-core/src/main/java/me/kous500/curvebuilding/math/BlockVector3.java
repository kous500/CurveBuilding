package me.kous500.curvebuilding.math;

/**
 * This class is compatible with com.sk89q.worldedit.math.BlockVector3.
 * An immutable 3-dimensional vector.
 */
public record BlockVector3(int x, int y, int z) {
    public static BlockVector3 at(double x, double y, double z) {
        return at((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    public static BlockVector3 at(int x, int y, int z) {
        return new BlockVector3(x, y, z);
    }

    public static BlockVector3 floor(Vector3 vector3) {
        return at(Math.floor(vector3.x()), Math.floor(vector3.y()), Math.floor(vector3.z()));
    }

    public static BlockVector3 round(Vector3 vector3) {
        return at(Math.round(vector3.x()), Math.round(vector3.y()), Math.round(vector3.z()));
    }

    public static BlockVector3 ceil(Vector3 vector3) {
        return at(Math.ceil(vector3.x()), Math.ceil(vector3.y()), Math.ceil(vector3.z()));
    }

    private static final int WORLD_XZ_MINMAX = 30_000_000;
    private static final int WORLD_Y_MIN = -2048;
    private static final int WORLD_Y_MAX = 2047;

    private static boolean isHorizontallyInBounds(int h) {
        return -WORLD_XZ_MINMAX <= h && h <= WORLD_XZ_MINMAX;
    }

    public static boolean isLongPackable(BlockVector3 location) {
        return isHorizontallyInBounds(location.x)
                && isHorizontallyInBounds(location.z)
                && WORLD_Y_MIN <= location.y && location.y <= WORLD_Y_MAX;
    }

    /**
     * Set the X coordinate.
     *
     * @param x the new X
     * @return a new vector
     */
    public BlockVector3 withX(int x) {
        return at(x, y, z);
    }

    /**
     * Set the Y coordinate.
     *
     * @param y the new Y
     * @return a new vector
     */
    public BlockVector3 withY(int y) {
        return at(x, y, z);
    }

    /**
     * Set the Z coordinate.
     *
     * @param z the new Z
     * @return a new vector
     */
    public BlockVector3 withZ(int z) {
        return at(x, y, z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 add(BlockVector3 other) {
        return add(other.x, other.y, other.z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param x the value to add
     * @param y the value to add
     * @param z the value to add
     * @return a new vector
     */
    public BlockVector3 add(int x, int y, int z) {
        return at(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Add a list of vectors to this vector and return the
     * result as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public BlockVector3 add(BlockVector3... others) {
        int newX = x;
        int newY = y;
        int newZ = z;

        for (BlockVector3 other : others) {
            newX += other.x;
            newY += other.y;
            newZ += other.z;
        }

        return at(newX, newY, newZ);
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 subtract(BlockVector3 other) {
        return subtract(other.x, other.y, other.z);
    }

    /**
     * Subtract another vector from this vector and return the result
     * as a new vector.
     *
     * @param x the value to subtract
     * @param y the value to subtract
     * @param z the value to subtract
     * @return a new vector
     */
    public BlockVector3 subtract(int x, int y, int z) {
        return at(this.x - x, this.y - y, this.z - z);
    }

    /**
     * Subtract a list of vectors from this vector and return the result
     * as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public BlockVector3 subtract(BlockVector3... others) {
        int newX = x;
        int newY = y;
        int newZ = z;

        for (BlockVector3 other : others) {
            newX -= other.x;
            newY -= other.y;
            newZ -= other.z;
        }

        return at(newX, newY, newZ);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 multiply(BlockVector3 other) {
        return multiply(other.x, other.y, other.z);
    }

    /**
     * Multiply this vector by another vector on each component.
     *
     * @param x the value to multiply
     * @param y the value to multiply
     * @param z the value to multiply
     * @return a new vector
     */
    public BlockVector3 multiply(int x, int y, int z) {
        return at(this.x * x, this.y * y, this.z * z);
    }

    /**
     * Multiply this vector by zero or more vectors on each component.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public BlockVector3 multiply(BlockVector3... others) {
        int newX = x;
        int newY = y;
        int newZ = z;

        for (BlockVector3 other : others) {
            newX *= other.x;
            newY *= other.y;
            newZ *= other.z;
        }

        return at(newX, newY, newZ);
    }

    /**
     * Perform scalar multiplication and return a new vector.
     *
     * @param n the value to multiply
     * @return a new vector
     */
    public BlockVector3 multiply(int n) {
        return multiply(n, n, n);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param other the other vector
     * @return a new vector
     */
    public BlockVector3 divide(BlockVector3 other) {
        return divide(other.x, other.y, other.z);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public BlockVector3 divide(int x, int y, int z) {
        return at(this.x / x, this.y / y, this.z / z);
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public BlockVector3 divide(int n) {
        return divide(n, n, n);
    }

    /**
     * Shift all components right.
     *
     * @param x the value to shift x by
     * @param y the value to shift y by
     * @param z the value to shift z by
     * @return a new vector
     */
    public BlockVector3 shr(int x, int y, int z) {
        return at(this.x >> x, this.y >> y, this.z >> z);
    }

    /**
     * Shift all components right by {@code n}.
     *
     * @param n the value to shift by
     * @return a new vector
     */
    public BlockVector3 shr(int n) {
        return shr(n, n, n);
    }

    /**
     * Shift all components left.
     *
     * @param x the value to shift x by
     * @param y the value to shift y by
     * @param z the value to shift z by
     * @return a new vector
     */
    public BlockVector3 shl(int x, int y, int z) {
        return at(this.x << x, this.y << y, this.z << z);
    }

    /**
     * Shift all components left by {@code n}.
     *
     * @param n the value to shift by
     * @return a new vector
     */
    public BlockVector3 shl(int n) {
        return shl(n, n, n);
    }

    /**
     * Get the length of the vector.
     *
     * @return length
     */
    public double length() {
        return Math.sqrt(lengthSq());
    }

    /**
     * Get the length, squared, of the vector.
     *
     * @return length, squared
     */
    public int lengthSq() {
        return x * x + y * y + z * z;
    }

    /**
     * Get the distance between this vector and another vector.
     *
     * @param other the other vector
     * @return distance
     */
    public double distance(BlockVector3 other) {
        return Math.sqrt(distanceSq(other));
    }

    /**
     * Get the distance between this vector and another vector, squared.
     *
     * @param other the other vector
     * @return distance
     */
    public int distanceSq(BlockVector3 other) {
        int dx = other.x - x;
        int dy = other.y - y;
        int dz = other.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length, as a new vector.
     *
     * @return a new vector
     */
    public BlockVector3 normalize() {
        double len = length();
        double x = this.x / len;
        double y = this.y / len;
        double z = this.z / len;
        return BlockVector3.at(x, y, z);
    }

    /**
     * Gets the dot product of this and another vector.
     *
     * @param other the other vector
     * @return the dot product of this and the other vector
     */
    public double dot(BlockVector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    /**
     * Gets the cross product of this and another vector.
     *
     * @param other the other vector
     * @return the cross product of this and the other vector
     */
    public BlockVector3 cross(BlockVector3 other) {
        return at(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x
        );
    }

    /**
     * Checks to see if a vector is contained with another.
     *
     * @param min the minimum point (X, Y, and Z are the lowest)
     * @param max the maximum point (X, Y, and Z are the lowest)
     * @return true if the vector is contained
     */
    public boolean containedWithin(BlockVector3 min, BlockVector3 max) {
        return x >= min.x && x <= max.x && y >= min.y && y <= max.y && z >= min.z && z <= max.z;
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    public BlockVector3 floor() {
        // already floored, kept for feature parity with Vector3
        return this;
    }

    /**
     * Rounds all components up.
     *
     * @return a new vector
     */
    public BlockVector3 ceil() {
        // already raised, kept for feature parity with Vector3
        return this;
    }

    /**
     * Rounds all components to the closest integer.
     *
     * <p>Components &lt; 0.5 are rounded down, otherwise up.</p>
     *
     * @return a new vector
     */
    public BlockVector3 round() {
        // already rounded, kept for feature parity with Vector3
        return this;
    }

    /**
     * Returns a vector with the absolute values of the components of
     * this vector.
     *
     * @return a new vector
     */
    public BlockVector3 abs() {
        return BlockVector3.at(Math.abs(x), Math.abs(y), Math.abs(z));
    }

    /**
     * Get this vector's pitch as used within the game.
     *
     * @return pitch in radians
     */
    public double toPitch() {
        double x = this.x;
        double z = this.z;

        if (x == 0 && z == 0) {
            return y > 0 ? -90 : 90;
        } else {
            double x2 = x * x;
            double z2 = z * z;
            double xz = Math.sqrt(x2 + z2);
            return Math.toDegrees(Math.atan(-y / xz));
        }
    }

    /**
     * Get this vector's yaw as used within the game.
     *
     * @return yaw in radians
     */
    public double toYaw() {
        double x = this.x;
        double z = this.z;

        double t = Math.atan2(-x, z);
        double tau = 2 * Math.PI;

        return Math.toDegrees(((t + tau) % tau));
    }

    /**
     * Gets the minimum components of two vectors.
     *
     * @param v2 the second vector
     * @return minimum
     */
    public BlockVector3 getMinimum(BlockVector3 v2) {
        return at(
                Math.min(x, v2.x),
                Math.min(y, v2.y),
                Math.min(z, v2.z)
        );
    }

    /**
     * Gets the maximum components of two vectors.
     *
     * @param v2 the second vector
     * @return maximum
     */
    public BlockVector3 getMaximum(BlockVector3 v2) {
        return at(
                Math.max(x, v2.x),
                Math.max(y, v2.y),
                Math.max(z, v2.z)
        );
    }

    public Vector3 toVector3() {
        return Vector3.at(x, y, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    /**
     * Returns a string representation that is supported by the parser.
     * @return string
     */
    public String toParserString() {
        return x + "," + y + "," + z;
    }
}
