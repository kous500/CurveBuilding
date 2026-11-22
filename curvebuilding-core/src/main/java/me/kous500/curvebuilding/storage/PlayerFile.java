package me.kous500.curvebuilding.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import me.kous500.curvebuilding.curve.CurvePoint;
import me.kous500.curvebuilding.curve.CurvePoint.Part;
import me.kous500.curvebuilding.math.Vector3;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.CharacterCodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static me.kous500.curvebuilding.log.Log.console;

/**
 * The JSON file of one player. It holds {@code version}, the version of its format; {@code current}, the curve the
 * player is working on; and other entries for other data of the player, such as named curves in {@code saved}, which
 * are written back as they are read. A curve is an object with its {@code type}, its {@code world} and the entries of
 * its type; the type {@code bezier} has the {@code points} of {@code //bcpos} by their number.
 */
final class PlayerFile {
    static final int VERSION = 1;
    static final String BEZIER = "bezier";
    private static final double MAX_COORDINATE = 30_000_000;
    private static final Pattern POINT_NUMBER = Pattern.compile("[1-9][0-9]*");
    private static final Pattern INTEGER = Pattern.compile("-?[0-9]+");
    // Without HTML escaping, so that < > & = ' in the text of the entries stay as they are
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private PlayerFile() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * A curve of the type {@code bezier}.
     *
     * @param world the ID of its world
     * @param points its points by their number
     */
    record Curve(String world, NavigableMap<Integer, CurvePoint> points) {
    }

    /**
     * What the file of a player holds. Nothing here is changed after it is made, so it can be written on another
     * thread.
     *
     * @param curve the curve the player is working on, or null
     * @param unknownCurve the curve the player is working on when its type is not {@code bezier}, as it was read, or
     *         null
     * @param others the entries besides {@code version} and {@code current}, in the order of the file
     * @param writable whether the file may be written: false when it could not be read, or is in a newer format
     */
    record Contents(@Nullable Curve curve, @Nullable JsonObject unknownCurve, JsonObject others, boolean writable) {
        /** Nothing, in a file that {@code writable} says whether to write. */
        static Contents empty(boolean writable) {
            return new Contents(null, null, new JsonObject(), writable);
        }

        /** These contents with {@code curve} as the curve the player is working on. */
        Contents withCurve(@Nullable Curve curve) {
            return new Contents(curve, null, others, writable);
        }
    }

    /**
     * Reads {@code file}. A file that is not JSON in UTF-8, or has no version of its format, is moved into
     * {@code backupFolder}. When parts of the curve cannot be used, the file is first copied there, and the rest is
     * read. Each of these is logged.
     *
     * @param file the file of the player
     * @param backupFolder the folder for the backups, created when needed
     * @param clock the clock the backups are named after
     * @return what the file holds; nothing when it does not exist
     */
    static Contents read(Path file, Path backupFolder, Clock clock) {
        if (!Files.exists(file)) return Contents.empty(true);

        String text;
        try {
            text = Files.readString(file);
        } catch (CharacterCodingException e) {
            return moveAside(file, backupFolder, clock, "is not UTF-8");
        } catch (IOException e) {
            console().warning("Could not read " + file + ", so it is not written while the player is on", e);
            return Contents.empty(false);
        }

        JsonObject root;
        try {
            root = parse(text);
        } catch (InvalidFileException e) {
            return moveAside(file, backupFolder, clock, e.getMessage());
        }

        JsonElement versionElement = root.get("version");
        String version = versionElement instanceof JsonPrimitive primitive && primitive.isNumber()
                ? primitive.getAsString()
                : null;
        if (version == null || !INTEGER.matcher(version).matches() || new BigInteger(version).signum() <= 0) {
            return moveAside(file, backupFolder, clock, "has no version of its format");
        }
        if (new BigInteger(version).compareTo(BigInteger.valueOf(VERSION)) > 0) {
            console().warning(file + " is in the format of version " + version + ", newer than this version reads, so "
                    + "it is not read, and not written while the player is on");
            return Contents.empty(false);
        }

        JsonObject others = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (!entry.getKey().equals("version") && !entry.getKey().equals("current")) {
                others.add(entry.getKey(), entry.getValue());
            }
        }

        List<String> problems = new ArrayList<>();
        Curve curve = null;
        JsonObject unknownCurve = null;
        JsonElement current = root.get("current");
        if (current instanceof JsonObject object) {
            String type = string(object.get("type"));
            if (type == null) {
                problems.add("current has no type, so the curve was left out");
            } else if (type.equals(BEZIER)) {
                curve = bezier(object, problems);
            } else {
                console().warning(file + " has a curve of the type " + type + ", which this version does not know. "
                        + "It stays in the file until the player sets a new curve.");
                unknownCurve = object;
            }
        } else if (current != null && !current.isJsonNull()) {
            problems.add("current is not an object, so the curve was left out");
        }

        boolean writable = problems.isEmpty() || backUp(file, backupFolder, clock, problems);
        return new Contents(curve, unknownCurve, others, writable);
    }

    /**
     * The text of the file with {@code contents}: {@code version} first, then {@code current}, then the other
     * entries. Objects are indented by two spaces, and arrays of single values are written on one line, such as
     * {@code [10.5, 64.5, 0.5]}.
     *
     * @param contents what the file holds
     * @return the text, or null when there is nothing to keep but the version
     */
    static @Nullable String text(Contents contents) {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        Curve curve = contents.curve();
        if (curve != null) {
            root.add("current", json(curve));
        } else if (contents.unknownCurve() != null) {
            root.add("current", contents.unknownCurve());
        }
        for (Map.Entry<String, JsonElement> entry : contents.others().entrySet()) {
            root.add(entry.getKey(), entry.getValue());
        }
        if (root.size() == 1) return null;

        StringBuilder text = new StringBuilder();
        append(text, root, "");
        return text.append('\n').toString();
    }

    /** The object of {@code text}, read strictly: no comments, no names without quotes and no NaN. */
    private static JsonObject parse(String text) throws InvalidFileException {
        // A new JsonReader is strict. Gson.fromJson and JsonParser.parseReader make it lenient while they read
        // (Gson 2.10), so the adapter reads it instead.
        JsonReader reader = new JsonReader(new StringReader(text.startsWith("﻿") ? text.substring(1) : text));
        JsonElement root;
        try {
            root = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new InvalidFileException("has more after its JSON");
            }
        } catch (IOException | JsonParseException | IllegalStateException e) {
            throw new InvalidFileException("could not be read as JSON: " + e.getMessage());
        }
        if (!(root instanceof JsonObject object)) {
            throw new InvalidFileException("does not hold a JSON object");
        }
        return object;
    }

    /** The bezier curve of {@code current}, or null when it cannot be used; the parts left out go to problems. */
    private static @Nullable Curve bezier(JsonObject current, List<String> problems) {
        String world = string(current.get("world"));
        if (world == null) {
            problems.add("current has no world, so the curve was left out");
            return null;
        }
        if (!(current.get("points") instanceof JsonObject pointsObject)) {
            problems.add("current has no points, so the curve was left out");
            return null;
        }

        NavigableMap<Integer, CurvePoint> points = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : pointsObject.entrySet()) {
            String key = entry.getKey();
            Integer n = pointNumber(key);
            if (n == null) {
                problems.add("the point " + key + " is not a number from 1, so it was left out");
                continue;
            }
            if (!(entry.getValue() instanceof JsonObject partsObject)) {
                problems.add("the point " + key + " is not an object, so it was left out");
                continue;
            }

            CurvePoint point = CurvePoint.EMPTY;
            for (Part part : Part.values()) {
                JsonElement value = partsObject.get(name(part));
                if (value == null || value.isJsonNull()) continue;
                Vector3 position = position(value);
                if (position == null) {
                    problems.add(name(part) + " of the point " + key + " is not three numbers from -"
                            + (long) MAX_COORDINATE + " to " + (long) MAX_COORDINATE + ", so it was left out");
                } else {
                    point = point.with(part, position);
                }
            }
            points.put(n, point);
        }
        return new Curve(world, points);
    }

    private static JsonObject json(Curve curve) {
        JsonObject points = new JsonObject();
        curve.points().forEach((n, point) -> {
            JsonObject parts = new JsonObject();
            for (Part part : Part.values()) {
                Vector3 position = point.get(part);
                if (position != null) {
                    JsonArray coordinates = new JsonArray();
                    coordinates.add(position.x());
                    coordinates.add(position.y());
                    coordinates.add(position.z());
                    parts.add(name(part), coordinates);
                }
            }
            points.add(String.valueOf(n), parts);
        });

        JsonObject current = new JsonObject();
        current.addProperty("type", BEZIER);
        current.addProperty("world", curve.world());
        current.add("points", points);
        return current;
    }

    /** The name of {@code part} in the file: {@code point}, {@code f} or {@code b}, as in {@code //bcpos}. */
    private static String name(Part part) {
        return part.name().toLowerCase(Locale.ROOT);
    }

    /** The number of a point, or null when {@code key} is not a number from 1 that fits in an int. */
    private static @Nullable Integer pointNumber(String key) {
        if (!POINT_NUMBER.matcher(key).matches()) return null;
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** The position of three numbers, or null when {@code element} is not three numbers within the coordinates. */
    private static @Nullable Vector3 position(JsonElement element) {
        if (!(element instanceof JsonArray array) || array.size() != 3) return null;
        double[] coordinates = new double[3];
        for (int i = 0; i < 3; i++) {
            if (!(array.get(i) instanceof JsonPrimitive primitive) || !primitive.isNumber()) return null;
            double value = primitive.getAsDouble();
            // A number too large for a double reads as an infinity
            if (!(Math.abs(value) <= MAX_COORDINATE)) return null;
            coordinates[i] = value;
        }
        return Vector3.at(coordinates[0], coordinates[1], coordinates[2]);
    }

    private static @Nullable String string(@Nullable JsonElement element) {
        return element instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : null;
    }

    /**
     * Copies {@code file} into {@code backupFolder} and logs {@code problems}.
     *
     * @return whether it was copied, so that the file may be written
     */
    private static boolean backUp(Path file, Path backupFolder, Clock clock, List<String> problems) {
        for (String problem : problems) {
            console().warning(file + ": " + problem);
        }
        Path backup = SafeFiles.backupPath(file, backupFolder, clock);
        try {
            Files.createDirectories(backupFolder);
            Files.copy(file, backup);
        } catch (IOException e) {
            console().warning("Could not copy " + file + " into " + backupFolder + ", so it is not written while "
                    + "the player is on", e);
            return false;
        }
        console().info("The previous " + file + " is saved as " + backup);
        return true;
    }

    /** Moves {@code file}, which cannot be read, into {@code backupFolder}, and logs {@code problem}. */
    private static Contents moveAside(Path file, Path backupFolder, Clock clock, String problem) {
        Path backup = SafeFiles.backupPath(file, backupFolder, clock);
        try {
            Files.createDirectories(backupFolder);
            Files.move(file, backup);
        } catch (IOException e) {
            console().warning(file + " " + problem + ", and could not be moved into " + backupFolder + ", so it is "
                    + "not written while the player is on", e);
            return Contents.empty(false);
        }
        console().warning(file + " " + problem + ", so it was moved to " + backup + " and the player has no curve");
        return Contents.empty(true);
    }

    private static void append(StringBuilder text, JsonElement element, String indent) {
        if (element instanceof JsonObject object) {
            if (object.size() == 0) {
                text.append("{}");
                return;
            }
            String inner = indent + "  ";
            text.append("{\n");
            boolean first = true;
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (!first) text.append(",\n");
                first = false;
                text.append(inner).append(GSON.toJson(new JsonPrimitive(entry.getKey()))).append(": ");
                append(text, entry.getValue(), inner);
            }
            text.append('\n').append(indent).append('}');
        } else if (element instanceof JsonArray array) {
            appendArray(text, array, indent);
        } else {
            text.append(GSON.toJson(element));
        }
    }

    private static void appendArray(StringBuilder text, JsonArray array, String indent) {
        boolean singleValues = true;
        for (JsonElement element : array) {
            singleValues &= element.isJsonPrimitive() || element.isJsonNull();
        }
        if (singleValues) {
            text.append('[');
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) text.append(", ");
                text.append(GSON.toJson(array.get(i)));
            }
            text.append(']');
            return;
        }

        String inner = indent + "  ";
        text.append("[\n");
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) text.append(",\n");
            text.append(inner);
            append(text, array.get(i), inner);
        }
        text.append('\n').append(indent).append(']');
    }

    /** A file that cannot be read, with what is wrong with it, to follow its name. */
    private static final class InvalidFileException extends Exception {
        InvalidFileException(String message) {
            super(message);
        }
    }
}
