package me.kous500.curvebuilding.preview;

import me.kous500.curvebuilding.math.CurvePoint;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.preview.ParticleLayout.Kind;
import me.kous500.curvebuilding.preview.ParticleLayout.Particle;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleLayoutTest {
    private static final ParticleLayout.Settings SETTINGS = new ParticleLayout.Settings(4, 4, 10000);
    private static final Vector3 VIEWER = Vector3.at(0, 64, 0);

    private static NavigableMap<Integer, CurvePoint> points(CurvePoint... points) {
        NavigableMap<Integer, CurvePoint> map = new TreeMap<>();
        for (int n = 1; n <= points.length; n++) map.put(n, points[n - 1]);
        return map;
    }

    private static long count(List<Particle> particles, Kind kind) {
        return particles.stream().filter(particle -> particle.kind() == kind).count();
    }

    @Test
    void pointHasTheEdgesOfItsBlockAndACenter() {
        List<Particle> particles = ParticleLayout.layout(
                points(new CurvePoint(Vector3.at(10, 64, 10), null, null)), VIEWER, 0, SETTINGS);

        // 8 corners and 3 more particles on each of the 12 edges, with 4 particles per block
        assertEquals(8 + 12 * 3, count(particles, Kind.START));
        assertEquals(1, count(particles, Kind.CENTER));
        for (Particle particle : particles) {
            Vector3 position = particle.position();
            assertTrue(position.x() >= 10 && position.x() <= 11 && position.z() >= 10 && position.z() <= 11,
                    particle.toString());
        }
    }

    @Test
    void firstAndLastPointsAreToldApart() {
        Vector3 a = Vector3.at(0, 64, 0);
        List<Particle> particles = ParticleLayout.layout(points(
                new CurvePoint(a, null, null), new CurvePoint(a.add(5, 0, 0), null, null),
                new CurvePoint(a.add(10, 0, 0), null, null)), VIEWER, 0, SETTINGS);

        assertEquals(44, count(particles, Kind.START));
        assertEquals(44, count(particles, Kind.POINT));
        assertEquals(44, count(particles, Kind.END));
    }

    @Test
    void handleHasACrossToTheCenterAndALine() {
        Vector3 point = Vector3.at(0, 64, 0);
        List<Particle> particles = ParticleLayout.layout(
                points(new CurvePoint(point, null, point.add(6, 0, 0))), VIEWER, 0, SETTINGS);

        // 8 lines from the corners to the center, 4 * sqrt(3) / 2 = 3 steps each, both ends included
        assertEquals(8 * 4, count(particles, Kind.B));
        assertEquals(2, count(particles, Kind.CENTER));
        assertTrue(count(particles, Kind.HANDLE_LINE) > 0);
    }

    @Test
    void crossWithTheLowestDensityHasItsCornersAndCenter() {
        Vector3 point = Vector3.at(0, 64, 0);
        ParticleLayout.Settings lowest = new ParticleLayout.Settings(1, 1, 10000);
        List<Particle> particles = ParticleLayout.layout(
                points(new CurvePoint(point, null, point.add(6, 0, 0))), VIEWER, 0, lowest);

        List<Particle> cross = particles.stream().filter(particle -> particle.kind() == Kind.B).toList();
        assertEquals(8 * 2, cross.size());
        for (Particle particle : cross) {
            assertTrue(Double.isFinite(particle.position().x()), particle.toString());
        }
    }

    @Test
    void turnsSplitTheDotsOfTheLines() {
        Vector3 a = Vector3.at(0, 64, 0);
        NavigableMap<Integer, CurvePoint> curve = points(
                new CurvePoint(a, null, a.add(0, 0, 8)), new CurvePoint(a.add(20, 0, 0), a.add(20, 0, 8), null));

        // Two lines can meet at a point, so each line is checked on its own
        Set<Particle> all = new HashSet<>();
        int dots = 0;
        for (int turn = 0; turn < ParticleLayout.TURNS; turn++) {
            for (Particle particle : ParticleLayout.layout(curve, VIEWER, turn, SETTINGS)) {
                if (particle.kind() == Kind.CURVE) {
                    assertTrue(all.add(particle), "a dot in two turns: " + particle);
                    dots++;
                }
            }
        }
        assertTrue(dots > ParticleLayout.TURNS, String.valueOf(dots));
    }

    @Test
    void dotsAreFurtherApartFarFromTheViewer() {
        Vector3 a = Vector3.at(0, 64, 0);
        NavigableMap<Integer, CurvePoint> line = points(
                new CurvePoint(a, null, null), new CurvePoint(a.add(30, 0, 0), null, null));

        long near = count(ParticleLayout.layout(line, Vector3.at(15, 64, 0), 0, SETTINGS), Kind.CURVE);
        long far = count(ParticleLayout.layout(line, Vector3.at(15, 64, 3000), 0, SETTINGS), Kind.CURVE);

        assertTrue(far < near, near + " near, " + far + " far");
    }

    @Test
    void curveLongerThanTheMaximumIsNotShown() {
        Vector3 a = Vector3.at(0, 64, 0);
        NavigableMap<Integer, CurvePoint> line = points(
                new CurvePoint(a, null, null), new CurvePoint(a.add(30, 0, 0), null, null));

        ParticleLayout.Settings shorter = new ParticleLayout.Settings(4, 4, 20);
        ParticleLayout.Settings longer = new ParticleLayout.Settings(4, 4, 40);
        assertEquals(0, count(ParticleLayout.layout(line, VIEWER, 0, shorter), Kind.CURVE));
        assertTrue(count(ParticleLayout.layout(line, VIEWER, 0, longer), Kind.CURVE) > 0);
    }
}
