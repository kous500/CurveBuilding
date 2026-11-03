package me.kous500.curvebuilding.math;

/**
 * An immutable 3-dimensional vector, with methods named like those of the {@code Vector3} of WorldEdit.
 *
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 */
public record Vector3(double x, double y, double z) {
    /**
     * Create a new {@code Vector3}.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a new vector
     */
    public static Vector3 at(double x, double y, double z) {
        return new Vector3(x, y, z);
    }

    /**
     * Get the X coordinate, aligned to the block grid.
     *
     * @return the block-aligned x coordinate
     */
    public int blockX() {
        return (int) Math.floor(x);
    }

    /**
     * Get the Y coordinate, aligned to the block grid.
     *
     * @return the block-aligned y coordinate
     */
    public int blockY() {
        return (int) Math.floor(y);
    }

    /**
     * Get the Z coordinate, aligned to the block grid.
     *
     * @return the block-aligned z coordinate
     */
    public int blockZ() {
        return (int) Math.floor(z);
    }

    /**
     * Add another vector to this vector and return the result as a new vector.
     *
     * @param other the other vector
     * @return a new vector
     */
    public Vector3 add(Vector3 other) {
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
    public Vector3 add(double x, double y, double z) {
        return at(this.x + x, this.y + y, this.z + z);
    }

    /**
     * Add a list of vectors to this vector and return the
     * result as a new vector.
     *
     * @param others an array of vectors
     * @return a new vector
     */
    public Vector3 add(Vector3... others) {
        double newX = x;
        double newY = y;
        double newZ = z;

        for (Vector3 other : others) {
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
    public Vector3 subtract(Vector3 other) {
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
    public Vector3 subtract(double x, double y, double z) {
        return at(this.x - x, this.y - y, this.z - z);
    }

    /**
     * Divide this vector by another vector on each component.
     *
     * @param x the value to divide by
     * @param y the value to divide by
     * @param z the value to divide by
     * @return a new vector
     */
    public Vector3 divide(double x, double y, double z) {
        return at(this.x / x, this.y / y, this.z / z);
    }

    /**
     * Perform scalar division and return a new vector.
     *
     * @param n the value to divide by
     * @return a new vector
     */
    public Vector3 divide(double n) {
        return divide(n, n, n);
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
     * This vector scaled to {@code length}, keeping its direction; a negative length turns it the other way. A zero
     * vector stays zero.
     *
     * @param length the length to scale to
     * @return a new vector
     */
    public Vector3 withLength(double length) {
        double current = length();
        if (current == 0) return at(0, 0, 0);

        double scale = length / current;
        return at(x * scale, y * scale, z * scale);
    }

    /**
     * Get the length, squared, of the vector.
     *
     * @return length, squared
     */
    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    /**
     * Get the distance between this vector and another vector.
     *
     * @param other the other vector
     * @return distance
     */
    public double distance(Vector3 other) {
        return Math.sqrt(distanceSq(other));
    }

    /**
     * Get the distance between this vector and another vector, squared.
     *
     * @param other the other vector
     * @return distance
     */
    public double distanceSq(Vector3 other) {
        double dx = other.x - x;
        double dy = other.y - y;
        double dz = other.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Get the normalized vector, which is the vector divided by its
     * length, as a new vector.
     *
     * @return a new vector
     */
    public Vector3 normalize() {
        return divide(length());
    }

    /**
     * Floors the values of all components.
     *
     * @return a new vector
     */
    public Vector3 floor() {
        return at(Math.floor(x), Math.floor(y), Math.floor(z));
    }

    /**
     * Create a new {@code BlockVector} using the given components.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a new {@code BlockVector}
     */
    public static BlockVector3 toBlockPoint(double x, double y, double z) {
        return BlockVector3.at(x, y, z);
    }

    /**
     * Create a new {@code BlockVector} from this vector.
     *
     * @return a new {@code BlockVector}
     */
    public BlockVector3 toBlockPoint() {
        return toBlockPoint(x, y, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
