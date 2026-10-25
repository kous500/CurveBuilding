package me.kous500.curvebuilding;

import me.kous500.curvebuilding.math.Vector3;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import static java.lang.Math.*;

/**
 * このプラグインで共通で使う処理をまとめています。
 */
public final class Util {

    private Util() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * pos1からpos4の値からベジエ曲線の長さを求める。
     *
     * @param pos pos1からpos4の値
     * @param fineness 分割数（精度）
     * @return 曲線の長さ
     */
    public static double bezierLength(Vector3[] pos, double fineness) {
        validateBezierPositions(pos);
        if (fineness <= 0) {
            throw new IllegalArgumentException("fineness must be greater than 0");
        }

        double length = 0;
        Vector3 start = bezierCoordinate(pos, 0);
        double xt1 = start.x();
        double yt1 = start.y();
        double zt1 = start.z();

        for (int step = 1; step <= fineness; step++) {
            double i = step / fineness;
            Vector3 p = bezierCoordinate(pos, i);
            double xt = p.x();
            double yt = p.y();
            double zt = p.z();

            length += Math.sqrt((xt - xt1) * (xt - xt1) + (yt - yt1) * (yt - yt1) + (zt - zt1) * (zt - zt1));
            xt1 = xt;
            yt1 = yt;
            zt1 = zt;
        }
        return length;
    }

    /**
     * pos1からpos4の値から生成されるベジエ曲線の曲線上の位置iの座標を求める。
     *
     * @param pos pos1からpos4までの値
     * @param i 曲線上の位置(0 <= i <= 1)
     * @return 座標
     */
    public static Vector3 bezierCoordinate(Vector3[] pos, double i) {
        validateBezierPositions(pos);
        if (i < 0 || i > 1) {
            throw new IllegalArgumentException("The value of i must be in the range 0 to 1");
        }

        Vector3 p0 = pos[0];
        Vector3 p1 = pos[1];
        Vector3 p2 = pos[2];
        Vector3 p3 = pos[3];

        double u = 1 - i;
        double bernstein3_0 = u * u * u;
        double bernstein3_1 = 3 * u * u * i;
        double bernstein3_2 = 3 * u * i * i;
        double bernstein3_3 = i * i * i;

        double x = bernstein3_0 * p0.x() + bernstein3_1 * p1.x() + bernstein3_2 * p2.x() + bernstein3_3 * p3.x() + 0.5;
        double y = bernstein3_0 * p0.y() + bernstein3_1 * p1.y() + bernstein3_2 * p2.y() + bernstein3_3 * p3.y() + 0.5;
        double z = bernstein3_0 * p0.z() + bernstein3_1 * p1.z() + bernstein3_2 * p2.z() + bernstein3_3 * p3.z() + 0.5;

        return Vector3.at(x, y, z);
    }

    /**
     * Splits the curve through positions 1, 2, ... into cubic Bézier segments, one per pair of consecutive positions.
     * The segment from position n - 1 to n is {position n - 1, handle 2 of n - 1, handle 1 of n, position n}; a missing
     * handle is replaced by its position. Stops at the first missing position.
     *
     * @param posMap positions by number; each value is {position, handle 1, handle 2}
     * @return the segments in order, empty when there are fewer than two positions or {@code posMap} is null
     */
    public static List<Vector3[]> bezierSegments(NavigableMap<Integer, Vector3[]> posMap) {
        List<Vector3[]> segments = new ArrayList<>();
        if (posMap == null || posMap.isEmpty()) return segments;

        for (int n = 1; n <= posMap.lastKey(); n++) {
            Vector3[] p = posMap.get(n);
            if (p == null || p[0] == null) break;

            if (n > 1) {
                Vector3[] bp = posMap.get(n - 1);
                segments.add(new Vector3[]{
                        bp[0],
                        bp[2] != null ? bp[2] : bp[0],
                        p[1] != null ? p[1] : p[0],
                        p[0]
                });
            }
        }
        return segments;
    }

    /**
     * Whether the ends of a segment, and each end and its handle, are at most {@code maxLength} apart.
     *
     * @param segment a segment from {@link #bezierSegments}
     */
    public static boolean isBezierSegmentWithin(Vector3[] segment, double maxLength) {
        return segment[0].distance(segment[3]) <= maxLength
                && segment[0].distance(segment[1]) <= maxLength
                && segment[3].distance(segment[2]) <= maxLength;
    }

    /**
     * Points on the curve at evenly spaced positions from 0 to 1, both ends included, as by {@link #bezierCoordinate}.
     *
     * @param pos the four control points of the curve
     * @param divisions number of parts to split the curve into; the result has {@code divisions + 1} points
     */
    public static List<Vector3> bezierPoints(Vector3[] pos, int divisions) {
        validateBezierPositions(pos);
        if (divisions < 1) {
            throw new IllegalArgumentException("divisions must be at least 1");
        }

        List<Vector3> points = new ArrayList<>(divisions + 1);
        for (int step = 0; step <= divisions; step++) {
            points.add(bezierCoordinate(pos, (double) step / divisions));
        }
        return points;
    }

    /**
     * ３次元の直交座標を極座標に変換したときの動径rを任意の大きさに変更する
     *
     * @param x x座標
     * @param y y座標
     * @param z z座標
     * @param newR 変更する動径rの大きさ
     * @return 動径rを変更した直交座標[ x, y, z ]
     */
    public static double[] changeR(double x, double y, double z, double newR) {
        double currentR = sqrt(x * x + y * y + z * z);

        if (currentR == 0) {
            return new double[]{0, 0, 0};
        }

        double scale = newR / currentR;
        return new double[]{ x * scale, y * scale, z * scale };
    }

    /**
     * 文字列内の{番号}を0から順に引数の単語に置き換える
     *
     * @param message 元の文字列
     * @param args 置き換える単語
     * @return 置き換え後の文字列
     */
    public static String messageReplace(String message, Object... args) {
        if (message == null || args == null) {
            return message;
        }

        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(args[i]));
        }

        return message;
    }

    private static void validateBezierPositions(Vector3[] pos) {
        Objects.requireNonNull(pos, "The pos array cannot be null");
        if (pos.length < 4 || pos[0] == null || pos[1] == null || pos[2] == null || pos[3] == null) {
            throw new IllegalArgumentException("The pos array must contain at least 4 non-null elements");
        }
    }
}