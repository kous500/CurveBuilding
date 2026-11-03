package me.kous500.curvebuilding.math;

/**
 * An immutable 3-dimensional vector of integers, with methods named like those of the {@code BlockVector3} of
 * WorldEdit.
 *
 * @param x the X coordinate
 * @param y the Y coordinate
 * @param z the Z coordinate
 */
public record BlockVector3(int x, int y, int z) implements Comparable<BlockVector3> {
    /**
     * Create a new {@code BlockVector3}, rounding each coordinate down.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a new vector
     */
    public static BlockVector3 at(double x, double y, double z) {
        return at((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    /**
     * Create a new {@code BlockVector3}.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return a new vector
     */
    public static BlockVector3 at(int x, int y, int z) {
        return new BlockVector3(x, y, z);
    }

    /**
     * The block that contains {@code vector3}: each component rounded down.
     *
     * @param vector3 the vector
     * @return a new vector
     */
    public static BlockVector3 floor(Vector3 vector3) {
        return at(Math.floor(vector3.x()), Math.floor(vector3.y()), Math.floor(vector3.z()));
    }

    /**
     * Each component of {@code vector3} rounded to the closest integer, halves up.
     *
     * @param vector3 the vector
     * @return a new vector
     */
    public static BlockVector3 round(Vector3 vector3) {
        return at(Math.round(vector3.x()), Math.round(vector3.y()), Math.round(vector3.z()));
    }

    /**
     * Each component of {@code vector3} rounded up.
     *
     * @param vector3 the vector
     * @return a new vector
     */
    public static BlockVector3 ceil(Vector3 vector3) {
        return at(Math.ceil(vector3.x()), Math.ceil(vector3.y()), Math.ceil(vector3.z()));
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
     * @param x the value to subtract
     * @param y the value to subtract
     * @param z the value to subtract
     * @return a new vector
     */
    public BlockVector3 subtract(int x, int y, int z) {
        return at(this.x - x, this.y - y, this.z - z);
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

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }

    @Override
    public int compareTo(BlockVector3 o) {
        return x != o.x() ? Integer.compare(x, o.x)
                : y != o.y() ? Integer.compare(y, o.y)
                : Integer.compare(z, o.z);
    }
}
