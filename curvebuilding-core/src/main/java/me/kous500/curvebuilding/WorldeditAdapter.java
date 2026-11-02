package me.kous500.curvebuilding;

import me.kous500.curvebuilding.math.BlockVector3;
import me.kous500.curvebuilding.math.Vector3;

import java.lang.reflect.Method;

/** Converts the vectors of WorldEdit to those of CurveBuilding and back, and reads the IDs of WorldEdit's worlds. */
public class WorldeditAdapter {
    // WorldEdit 7.2 has only getX() and getId(), and 7.3 added x() and id(). The methods are looked up by name, so
    // that one jar works with both. This can go once WorldEdit 7.2 is no longer supported.
    private static final MethodSuggestions<com.sk89q.worldedit.math.Vector3, Double> vector3_X = new MethodSuggestions<>(new String[]{"x", "getX"});
    private static final MethodSuggestions<com.sk89q.worldedit.math.Vector3, Double> vector3_Y = new MethodSuggestions<>(new String[]{"y", "getY"});
    private static final MethodSuggestions<com.sk89q.worldedit.math.Vector3, Double> vector3_Z = new MethodSuggestions<>(new String[]{"z", "getZ"});
    private static final MethodSuggestions<com.sk89q.worldedit.math.BlockVector3, Integer> blockVector3_X = new MethodSuggestions<>(new String[]{"x", "getX"});
    private static final MethodSuggestions<com.sk89q.worldedit.math.BlockVector3, Integer> blockVector3_Y = new MethodSuggestions<>(new String[]{"y", "getY"});
    private static final MethodSuggestions<com.sk89q.worldedit.math.BlockVector3, Integer> blockVector3_Z = new MethodSuggestions<>(new String[]{"z", "getZ"});
    private static final MethodSuggestions<com.sk89q.worldedit.world.World, String> world_id = new MethodSuggestions<>(new String[]{"id", "getId"});

    /**
     * The ID of {@code world}, which tells worlds apart.
     *
     * @param world the world
     * @return its ID
     */
    public static String getWorldId(com.sk89q.worldedit.world.World world) {
        return world_id.executeMethod(world);
    }

    /**
     * The vector of CurveBuilding with the components of a WorldEdit vector.
     *
     * @param vector3 the WorldEdit vector
     * @return the CurveBuilding vector
     */
    public static Vector3 adapt(com.sk89q.worldedit.math.Vector3 vector3) {
        return Vector3.at(
                vector3_X.executeMethod(vector3),
                vector3_Y.executeMethod(vector3),
                vector3_Z.executeMethod(vector3)
        );
    }

    /**
     * The block vector of CurveBuilding with the components of a WorldEdit block vector.
     *
     * @param blockVector3 the WorldEdit block vector
     * @return the CurveBuilding block vector
     */
    public static BlockVector3 adapt(com.sk89q.worldedit.math.BlockVector3 blockVector3) {
        return BlockVector3.at(
                blockVector3_X.executeMethod(blockVector3),
                blockVector3_Y.executeMethod(blockVector3),
                blockVector3_Z.executeMethod(blockVector3)
        );
    }

    /**
     * The WorldEdit vector with the components of a CurveBuilding vector.
     *
     * @param vector3 the CurveBuilding vector
     * @return the WorldEdit vector
     */
    public static com.sk89q.worldedit.math.Vector3 adapt(Vector3 vector3) {
        return com.sk89q.worldedit.math.Vector3.at(vector3.x(), vector3.y(), vector3.z());
    }

    /**
     * The WorldEdit block vector with the components of a CurveBuilding block vector.
     *
     * @param blockVector3 the CurveBuilding block vector
     * @return the WorldEdit block vector
     */
    public static com.sk89q.worldedit.math.BlockVector3 adapt(BlockVector3 blockVector3) {
        return com.sk89q.worldedit.math.BlockVector3.at(blockVector3.x(), blockVector3.y(), blockVector3.z());
    }

    private static class MethodSuggestions<C, R> {
        private Method method;
        private final String[] methodNames;

        MethodSuggestions(String[] methodNames) {
            this.methodNames = methodNames;
        }

        @SuppressWarnings("unchecked")
        R executeMethod(C instance, Object... args) {
            if (method == null) getMethod(instance, args);
            try {
                return (R) method.invoke(instance, args);
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute worldedit-api method.");
            }
        }

        private void getMethod(C instance, Object... args) {
            for (String methodName : methodNames) {
                try {
                    Class<?>[] argTypes = new Class<?>[args.length];
                    for (int i = 0; i < args.length; i++) {
                        argTypes[i] = args[i].getClass();
                    }
                    method = instance.getClass().getMethod(methodName, argTypes);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }

            if (method == null) throw new RuntimeException("Failed to convert the worldedit-api method.");
        }
    }
}
