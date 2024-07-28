package me.kous500.curvebuilding;

import me.kous500.curvebuilding.math.Vector3;

import static java.lang.Math.*;

/**
 * このプラグインで共通で使う処理をまとめています。
 */
public final class Util {

    /**
     * 誤差を考慮して実数をfloorで丸める。
     *
     * @param d 丸める数値
     * @param e 考慮する誤差
     * @return 丸められた値
     */
    public static double floorE(double d, double e) {
        if (d % 1 + e <= 1) {
            return Math.floor(d + e);
        } else if (d % 1 - e <= -1) {
            return Math.floor(d - e);
        } else {
            return Math.floor(d);
        }
    }

    /**
     * pos1からpos4の値からベジエ曲線の長さ求める。
     *
     * @param pos pos1からpos4の値
     * @return 曲線の長さ
     * @throws AssertionError p1からp4にnullが含まれる場合
     */
    public static double bezierLength(Vector3[] pos, double fineness) {
        assert (pos[0] != null) && (pos[1] != null) && (pos[2] != null) && (pos[3] != null)
                : "The value of pos cannot be null.";

        double length = 0;
        double xt1 = pos[0].x();
        double yt1 = pos[0].y();
        double zt1 = pos[0].z();

        for (double i = 0; i <= 1; i += 1.0 / fineness) {
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
     * @throws AssertionError p1からp4にnullが含まれる場合もしくはiが0以上1以下でない場合
     */
    public static Vector3 bezierCoordinate(Vector3[] pos, double i) {
        assert (pos[0] != null) && (pos[1] != null) && (pos[2] != null) && (pos[3] != null)
                : "The value of pos cannot be null.";
        assert (0 <= i) && (i <= 1)
                : "The value of i must be in the range 0 to 1";

        Vector3 p0 = pos[0];
        Vector3 p1 = pos[1];
        Vector3 p2 = pos[2];
        Vector3 p3 = pos[3];

        double x = pow(1-i, 3)*p0.x() + 3*pow(1-i, 2)*i*p1.x() + 3*(1-i)* pow(i, 2)*p2.x() + pow(i, 3)*p3.x() + 0.5;
        double y = pow(1-i, 3)*p0.y() + 3*pow(1-i, 2)*i*p1.y() + 3*(1-i)* pow(i, 2)*p2.y() + pow(i, 3)*p3.y() + 0.5;
        double z = pow(1-i, 3)*p0.z() + 3*pow(1-i, 2)*i*p1.z() + 3*(1-i)* pow(i, 2)*p2.z() + pow(i, 3)*p3.z() + 0.5;

        return Vector3.at(x, y, z);
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
        double[] polar = toPolar(x, y, z);
        double theta = polar[1];
        double phi = polar[2];
        double x1 = newR * sin(phi) * cos(theta);
        double y1 = newR * sin(phi) * sin(theta);
        double z1 = newR * cos(phi);
        return new double[]{ x1, y1, z1 };
    }

    /**
     * ３次元の直交座標を極座標（球座標）に変換する
     *
     * @param x x座標
     * @param y y座標
     * @param z z座標
     * @return 球座標[ r, θ, φ ]
     */
    public static double[] toPolar(double x, double y, double z) {
        double r = sqrt(x * x + y * y + z * z);
        double theta = atan2(y, x);
        double phi = acos(z / r);
        return new double[]{ r, theta, phi };
    }

    /**
     * 文字列内の{番号}を0から順に引数の単語に置き換える
     *
     * @param message 元の文字列
     * @param args 置き換える単語
     * @return 置き換え後の文字列
     */
    public static String messageReplace(String message, Object... args) {
        int i = 0;
        for (Object arg: args) {
            message = message.replace("{" + i + "}", arg.toString());
            i++;
        }

        return message;
    }
}