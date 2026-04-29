package me.kous500.curvebuilding.math;

import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.WorldeditAdapter;

import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static me.kous500.curvebuilding.Util.changeR;

/**
 * ひとつの曲線（複数の制御点群）の座標データを保持・操作するためのクラス。
 * プレイヤーに依存せず、独立してインスタンス化して使用できます。
 */
public class PosData {

    private World world;
    private String worldId;
    private NavigableMap<Integer, Vector3[]> p;

    public PosData() {
        this.p = new TreeMap<>();
    }

    public PosData(NavigableMap<Integer, Vector3[]> p) {
        this.p = p;
    }

    public void reset(World world) {
        this.world = world;
        this.worldId = WorldeditAdapter.getWorldId(world);
        this.p.clear();
    }

    public void updatePos(int n, int h, Vector3 location) {
        Vector3[] l = p.computeIfAbsent(n, k -> new Vector3[3]);
        Vector3[] la = p.get(n + 1);
        Vector3[] lb = p.get(n - 1);

        if (h == 0 && l[h] != null) {
            double dx = location.x() - l[h].x();
            double dy = location.y() - l[h].y();
            double dz = location.z() - l[h].z();

            if (l[1] != null) l[1] = l[1].add(dx, dy, dz);
            if (l[2] != null) l[2] = l[2].add(dx, dy, dz);
        }

        l[h] = location;

        if (h == 1 && la != null && la[0] != null && l[0] != null) {
            l[2] = posAutocompletion(l[0], l[1], l[2]);
        } else if (h == 2 && lb != null && lb[0] != null && l[0] != null) {
            l[1] = posAutocompletion(l[0], l[2], l[1]);
        }

        p.put(n, l);

        if (h == 0 && lb != null && lb[0] != null && lb[1] != null && lb[2] == null) {
            lb[2] = posAutocompletion(lb[0], lb[1], null);
            p.put(n - 1, lb);
        }
    }

    public boolean clearSpecificPos(int n, int h) {
        Vector3[] l = p.get(n);
        if (l == null) return false;

        if (h == 0) {
            p.put(n, new Vector3[3]);
        } else {
            l[h] = null;
        }
        return true;
    }

    public void insertNodeAt(int n) {
        NavigableMap<Integer, Vector3[]> newMap = new TreeMap<>();
        for (Map.Entry<Integer, Vector3[]> entry : p.entrySet()) {
            int key = entry.getKey();
            if (key < n) {
                newMap.put(key, entry.getValue());
            } else {
                newMap.put(key + 1, entry.getValue());
            }
        }
        this.p = newMap;
    }

    public void removeNodeAt(int n) {
        p.remove(n);
        NavigableMap<Integer, Vector3[]> newMap = new TreeMap<>();
        for (Map.Entry<Integer, Vector3[]> entry : p.entrySet()) {
            int key = entry.getKey();
            if (key < n) {
                newMap.put(key, entry.getValue());
            } else if (key > n) {
                newMap.put(key - 1, entry.getValue());
            }
        }
        this.p = newMap;
    }

    public void applyShift(Vector3 shiftVec) {
        for (Vector3[] vec : p.values()) {
            if (vec != null) {
                for (int j = 0; j < 3; j++) {
                    if (vec[j] != null) {
                        vec[j] = vec[j].add(shiftVec);
                    }
                }
            }
        }
    }

    public boolean containsPos(int n) {
        return p.containsKey(n);
    }

    public String getWorldId() {
        return worldId;
    }

    public World getWorld() {
        return world;
    }

    public NavigableMap<Integer, Vector3[]> getMap() {
        return p;
    }

    private static Vector3 posAutocompletion(Vector3 h0, Vector3 hIN, Vector3 hOUT) {
        double dx = hIN.x() - h0.x();
        double dy = hIN.y() - h0.y();
        double dz = hIN.z() - h0.z();

        if (hOUT == null) {
            return h0.add(-dx, -dy, -dz);
        } else {
            double[] newH = changeR(dx, dy, dz, -1 * h0.distance(hOUT));
            return h0.add(newH[0], newH[1], newH[2]);
        }
    }
}