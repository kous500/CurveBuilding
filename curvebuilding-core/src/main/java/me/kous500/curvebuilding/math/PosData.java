package me.kous500.curvebuilding.math;

import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.WorldeditAdapter;

import me.kous500.curvebuilding.math.CurvePoint.Part;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static me.kous500.curvebuilding.Util.changeR;

/**
 * The points of one curve by their number, from 1, and the world they are in. Each change replaces the points with a
 * new map that is never changed, so {@link #points()} can be read on any thread, such as for particles or packets.
 */
public class PosData {
    private volatile String worldId;
    private volatile NavigableMap<Integer, CurvePoint> points;

    public PosData() {
        this(new TreeMap<>());
    }

    public PosData(NavigableMap<Integer, CurvePoint> points) {
        this.points = Collections.unmodifiableNavigableMap(new TreeMap<>(points));
    }

    /** The points, which this never changes. */
    public NavigableMap<Integer, CurvePoint> points() {
        return points;
    }

    public boolean isIn(World world) {
        return worldId != null && worldId.equals(WorldeditAdapter.getWorldId(world));
    }

    /** Removes every point, and keeps the next ones in {@code world}. */
    public void reset(World world) {
        worldId = WorldeditAdapter.getWorldId(world);
        replace(new TreeMap<>());
    }

    public boolean contains(int n) {
        return points.containsKey(n);
    }

    /**
     * Sets {@code part} of point {@code n} to {@code position}. Moving a point moves its handles with it. Setting a handle
     * of a point between two others points the other handle the opposite way, so that the curve goes through smoothly;
     * the same is done for the handle of the previous point when its next point is set.
     */
    public void set(int n, Part part, Vector3 position) {
        NavigableMap<Integer, CurvePoint> next = new TreeMap<>(points);
        CurvePoint current = next.getOrDefault(n, CurvePoint.EMPTY);
        CurvePoint after = next.get(n + 1);
        CurvePoint before = next.get(n - 1);

        if (part == Part.POINT && current.point() != null) {
            current = current.moved(position.subtract(current.point()));
        }
        current = current.with(part, position);

        if (part == Part.F && hasPoint(after) && current.point() != null) {
            current = current.with(Part.B, opposite(current.point(), current.f(), current.b()));
        } else if (part == Part.B && hasPoint(before) && current.point() != null) {
            current = current.with(Part.F, opposite(current.point(), current.b(), current.f()));
        }
        next.put(n, current);

        if (part == Part.POINT && hasPoint(before) && before.f() != null && before.b() == null) {
            next.put(n - 1, before.with(Part.B, opposite(before.point(), before.f(), null)));
        }
        replace(next);
    }

    /**
     * Clears {@code part} of point {@code n}. Clearing the point clears its handles too, and keeps its number.
     *
     * @return whether point {@code n} was there
     */
    public boolean clear(int n, Part part) {
        CurvePoint current = points.get(n);
        if (current == null) return false;

        NavigableMap<Integer, CurvePoint> next = new TreeMap<>(points);
        next.put(n, part == Part.POINT ? CurvePoint.EMPTY : current.with(part, null));
        replace(next);
        return true;
    }

    /** Makes room for a new point {@code n}: the points from {@code n} on get the next numbers. */
    public void insertAt(int n) {
        NavigableMap<Integer, CurvePoint> next = new TreeMap<>();
        for (Map.Entry<Integer, CurvePoint> entry : points.entrySet()) {
            next.put(entry.getKey() < n ? entry.getKey() : entry.getKey() + 1, entry.getValue());
        }
        replace(next);
    }

    /** Removes point {@code n}: the points after it get the numbers before. */
    public void removeAt(int n) {
        NavigableMap<Integer, CurvePoint> next = new TreeMap<>();
        for (Map.Entry<Integer, CurvePoint> entry : points.entrySet()) {
            if (entry.getKey() < n) {
                next.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey() > n) {
                next.put(entry.getKey() - 1, entry.getValue());
            }
        }
        replace(next);
    }

    /** Moves every point and handle by {@code offset}. */
    public void move(Vector3 offset) {
        NavigableMap<Integer, CurvePoint> next = new TreeMap<>();
        points.forEach((n, point) -> next.put(n, point.moved(offset)));
        replace(next);
    }

    private void replace(NavigableMap<Integer, CurvePoint> next) {
        points = Collections.unmodifiableNavigableMap(next);
    }

    private static boolean hasPoint(CurvePoint point) {
        return point != null && point.point() != null;
    }

    /**
     * The handle opposite to {@code handle} around {@code point}: as far as {@code current}, the handle it replaces, or
     * as far as {@code handle} when there is none.
     */
    private static Vector3 opposite(Vector3 point, Vector3 handle, Vector3 current) {
        double dx = handle.x() - point.x();
        double dy = handle.y() - point.y();
        double dz = handle.z() - point.z();

        if (current == null) {
            return point.add(-dx, -dy, -dz);
        }
        double[] scaled = changeR(dx, dy, dz, -1 * point.distance(current));
        return point.add(scaled[0], scaled[1], scaled[2]);
    }
}
