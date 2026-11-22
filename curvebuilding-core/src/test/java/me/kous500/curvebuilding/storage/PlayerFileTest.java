package me.kous500.curvebuilding.storage;

import me.kous500.curvebuilding.RecordingConsole;
import me.kous500.curvebuilding.curve.CurvePoint;
import me.kous500.curvebuilding.log.Console;
import me.kous500.curvebuilding.log.Log;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.storage.PlayerFile.Contents;
import me.kous500.curvebuilding.storage.PlayerFile.Curve;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reads and writes the file of a player. */
class PlayerFileTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-22T10:30:00Z"), ZoneOffset.UTC);
    private static final String TEXT = """
            {
              "version": 1,
              "current": {
                "type": "bezier",
                "world": "world",
                "points": {
                  "1": {
                    "point": [10.5, 64.5, 0.5],
                    "b": [14.5, 64.5, 0.5]
                  },
                  "2": {
                    "point": [20.0, 64.5, 3.5]
                  },
                  "3": {}
                }
              }
            }
            """;

    @TempDir
    Path temp;

    private Path file;
    private Path backups;
    private Path backup;
    private Console previousConsole;
    private RecordingConsole console;

    @BeforeEach
    void setUp() {
        previousConsole = Log.console();
        console = new RecordingConsole();
        Log.setConsole(console);
        file = temp.resolve("players/player.json");
        backups = temp.resolve("backups/players");
        backup = backups.resolve("player-2026-11-22_10-30-00.json");
    }

    @AfterEach
    void restoreConsole() {
        Log.setConsole(previousConsole);
    }

    @Test
    void curveIsWrittenWithItsPointsInOrder() {
        Contents contents = Contents.empty(true).withCurve(curve());

        assertEquals(TEXT, PlayerFile.text(contents));
    }

    @Test
    void writtenCurveIsReadAsItWas() {
        NavigableMap<Integer, CurvePoint> points = new TreeMap<>(Map.of(
                7, new CurvePoint(Vector3.at(-0.25, -64, 1e7), Vector3.at(1.0 / 3, 2, 3), null),
                2, new CurvePoint(null, null, Vector3.at(-29_999_999.5, 320.5, 0))));
        write(PlayerFile.text(Contents.empty(true).withCurve(new Curve("New World_minecraft:overworld", points))));

        Contents contents = read();

        assertEquals(new Curve("New World_minecraft:overworld", points), contents.curve());
        assertTrue(contents.writable());
        assertEquals(List.of(), console.entries);
    }

    @Test
    void missingFileHasNoCurve() {
        assertEquals(Contents.empty(true), read());
        assertEquals(List.of(), console.entries);
    }

    @Test
    void fileWithNothingButTheVersionIsNotWritten() {
        assertNull(PlayerFile.text(Contents.empty(true)));
    }

    @TestFactory
    Stream<DynamicTest> fileThatCannotBeReadIsMovedAside() {
        return Stream.of(
                "",
                "{",
                "[]",
                "{\"version\": 1} {}",
                "// comment\n{\"version\": 1}",
                "{version: 1}",
                "{\"version\": 1, \"current\": {\"type\": \"bezier\", \"world\": \"w\", \"points\": "
                        + "{\"1\": {\"point\": [NaN, 0, 0]}}}}",
                "{}",
                "{\"version\": \"1\"}",
                "{\"version\": 1.0}",
                "{\"version\": 0}"
        ).map(text -> DynamicTest.dynamicTest(text.isEmpty() ? "empty" : text, () -> {
            console.entries.clear();
            write(text);
            Files.deleteIfExists(backup);

            assertEquals(Contents.empty(true), read());
            assertFalse(Files.exists(file));
            assertEquals(text, Files.readString(backup));
            assertEquals(1, console.entries.size());
            assertTrue(console.entries.getFirst().message().startsWith(file + " "));
            assertTrue(console.entries.getFirst().message().endsWith(
                    ", so it was moved to " + backup + " and the player has no curve"));
        }));
    }

    @Test
    void fileThatIsNotUtf8IsMovedAside() throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, new byte[]{'{', (byte) 0xFF, '}'});

        assertEquals(Contents.empty(true), read());
        assertFalse(Files.exists(file));
        assertTrue(Files.exists(backup));
        assertLogs("warning: " + file + " is not UTF-8, so it was moved to " + backup + " and the player has no curve");
    }

    @Test
    void fileOfANewerVersionIsNeitherReadNorWritten() throws IOException {
        String text = TEXT.replace("\"version\": 1", "\"version\": 2");
        write(text);

        Contents contents = read();

        assertNull(contents.curve());
        assertFalse(contents.writable());
        assertEquals(text, Files.readString(file));
        assertFalse(Files.exists(backups));
        assertLogs("warning: " + file + " is in the format of version 2, newer than this version reads, so it is not "
                + "read, and not written while the player is on");
    }

    @Test
    void partsThatCannotBeUsedAreLeftOutAfterABackup() throws IOException {
        String text = """
                {
                  "version": 1,
                  "current": {
                    "type": "bezier",
                    "world": "world",
                    "points": {
                      "1": {"point": [1, 2, 3], "f": [1, 2], "b": null},
                      "0": {"point": [1, 2, 3]},
                      "01": {},
                      "2147483648": {},
                      "a": {},
                      "2": [1, 2, 3],
                      "3": {"point": ["1", 2, 3], "b": [30000000, -30000000, 0], "f": [30000000.5, 0, 0]},
                      "4": {"other": true}
                    }
                  }
                }
                """;
        write(text);

        Contents contents = read();

        NavigableMap<Integer, CurvePoint> points = new TreeMap<>(Map.of(
                1, new CurvePoint(Vector3.at(1, 2, 3), null, null),
                3, new CurvePoint(null, null, Vector3.at(30_000_000, -30_000_000, 0)),
                4, CurvePoint.EMPTY));
        assertEquals(new Curve("world", points), contents.curve());
        assertTrue(contents.writable());
        assertEquals(text, Files.readString(file));
        assertEquals(text, Files.readString(backup));
        assertLogs(
                "warning: " + file + ": f of the point 1 is not three numbers from -30000000 to 30000000, so it was "
                        + "left out",
                "warning: " + file + ": the point 0 is not a number from 1, so it was left out",
                "warning: " + file + ": the point 01 is not a number from 1, so it was left out",
                "warning: " + file + ": the point 2147483648 is not a number from 1, so it was left out",
                "warning: " + file + ": the point a is not a number from 1, so it was left out",
                "warning: " + file + ": the point 2 is not an object, so it was left out",
                "warning: " + file + ": point of the point 3 is not three numbers from -30000000 to 30000000, so it "
                        + "was left out",
                "warning: " + file + ": f of the point 3 is not three numbers from -30000000 to 30000000, so it was "
                        + "left out",
                "info: The previous " + file + " is saved as " + backup);
    }

    @TestFactory
    Stream<DynamicTest> curveThatCannotBeUsedIsLeftOutAndTheRestIsKept() {
        return Stream.of(
                new String[]{"\"current\": 1", "current is not an object"},
                new String[]{"\"current\": {\"world\": \"w\", \"points\": {}}", "current has no type"},
                new String[]{"\"current\": {\"type\": 1, \"world\": \"w\", \"points\": {}}", "current has no type"},
                new String[]{"\"current\": {\"type\": \"bezier\", \"points\": {}}", "current has no world"},
                new String[]{"\"current\": {\"type\": \"bezier\", \"world\": \"w\"}", "current has no points"},
                new String[]{"\"current\": {\"type\": \"bezier\", \"world\": \"w\", \"points\": []}",
                        "current has no points"}
        ).map(test -> DynamicTest.dynamicTest(test[0], () -> {
            console.entries.clear();
            Files.deleteIfExists(backup);
            write("{\"version\": 1, " + test[0] + ", \"saved\": {\"a\": 1}}");

            Contents contents = read();

            assertNull(contents.curve());
            assertNull(contents.unknownCurve());
            assertEquals("{\"saved\":{\"a\":1}}", contents.others().toString());
            assertTrue(Files.exists(backup));
            assertLogs(
                    "warning: " + file + ": " + test[1] + ", so the curve was left out",
                    "info: The previous " + file + " is saved as " + backup);
        }));
    }

    @Test
    void nullCurveIsNoCurve() {
        write("{\"version\": 1, \"current\": null}");

        assertEquals(Contents.empty(true), read());
        assertEquals(List.of(), console.entries);
    }

    @Test
    void curveOfAnUnknownTypeStaysUntilANewCurveIsSet() {
        String text = """
                {
                  "version": 1,
                  "current": {
                    "type": "arc",
                    "world": "world",
                    "center": [0.5, 64.5, 0.5],
                    "radius": 12.0
                  }
                }
                """;
        write(text);

        Contents contents = read();

        assertNull(contents.curve());
        assertNotNull(contents.unknownCurve());
        assertEquals(text, PlayerFile.text(contents));
        assertEquals(TEXT, PlayerFile.text(contents.withCurve(curve())));
        assertNull(PlayerFile.text(contents.withCurve(null)));
        assertLogs("warning: " + file + " has a curve of the type arc, which this version does not know. It stays in "
                + "the file until the player sets a new curve.");
    }

    @Test
    void otherEntriesAreWrittenBackAsTheyWereRead() {
        write("""
                {"saved": {"bridge": {"type": "bezier", "world": "w", "points": {"1": {"point": [1.50, 2e3, -0]}}}},
                 "version": 1, "note": "<a & b = 'c'>\\n", "list": [[1, {"x": null}], true, "\\u00e9"], "empty": [],
                 "current": {"type": "bezier", "world": "world", "points": {}}}
                """);

        Contents contents = read();

        assertEquals("""
                {
                  "version": 1,
                  "current": {
                    "type": "bezier",
                    "world": "world",
                    "points": {}
                  },
                  "saved": {
                    "bridge": {
                      "type": "bezier",
                      "world": "w",
                      "points": {
                        "1": {
                          "point": [1.50, 2e3, -0]
                        }
                      }
                    }
                  },
                  "note": "<a & b = 'c'>\\n",
                  "list": [
                    [
                      1,
                      {
                        "x": null
                      }
                    ],
                    true,
                    "é"
                  ],
                  "empty": []
                }
                """, PlayerFile.text(contents));
    }

    @Test
    void otherEntriesKeepTheFileWithoutACurve() {
        write("{\"version\": 1, \"saved\": {}}");

        Contents contents = read();

        assertEquals("""
                {
                  "version": 1,
                  "saved": {}
                }
                """, PlayerFile.text(contents.withCurve(null)));
    }

    private static Curve curve() {
        NavigableMap<Integer, CurvePoint> points = new TreeMap<>(Map.of(
                3, CurvePoint.EMPTY,
                1, new CurvePoint(Vector3.at(10.5, 64.5, 0.5), null, Vector3.at(14.5, 64.5, 0.5)),
                2, new CurvePoint(Vector3.at(20, 64.5, 3.5), null, null)));
        return new Curve("world", points);
    }

    private Contents read() {
        return PlayerFile.read(file, backups, CLOCK);
    }

    private void write(String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertLogs(String... expected) {
        List<String> actual = console.entries.stream().map(entry -> entry.level() + ": " + entry.message()).toList();
        assertEquals(List.of(expected), actual);
    }
}
