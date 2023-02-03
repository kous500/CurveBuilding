package com.kous.curvebuilding;


import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.jetbrains.annotations.NotNull;

import static java.lang.Math.pow;

/**
 * このプラグインで共通で使う処理をまとめています。
 */
public final class Util {
    /**
     * Vector3をroundで丸める。
     */
    public static BlockVector3 roundVector(@NotNull Vector3 vec){
        return BlockVector3.at(Math.round(vec.getX()), Math.round(vec.getY()), Math.round(vec.getZ()));
    }

    /**
     * Vector3をfloorで丸める。
     */
    public static BlockVector3 floorVector(@NotNull Vector3 vec){
        double e = 0.0001;
        return BlockVector3.at(floorE(vec.getX(), e), floorE(vec.getY(), e), floorE(vec.getZ(), e));
    }

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
    public static double bezierLength(Vector3 @NotNull [] pos, double fineness) {
        assert (pos[0] != null) && (pos[1] != null) && (pos[2] != null) && (pos[3] != null)
                : "値がnullのposがあります";

        double length = 0;
        double xt1 = pos[0].getX();
        double yt1 = pos[0].getY();
        double zt1 = pos[0].getZ();

        for (double i = 0; i <= 1; i += 1.0 / fineness) {
            Vector3 p = bezierCoordinate(pos, i);
            double xt = p.getX();
            double yt = p.getY();
            double zt = p.getZ();

            length += Math.sqrt((xt - xt1) * (xt - xt1) + (yt - yt1) * (yt - yt1) + (zt - zt1) * (zt - zt1));
            xt1 = xt;
            yt1 = yt;
            zt1 = zt;
        }
        return length;
    }

    /**
     * 始点と終点を直線で結んだときの長さを求める。
     *
     * @param pos1 始点
     * @param pos2 終点
     * @return 直線の長さ
     */
    public static double lineLength(@NotNull Vector3 pos1, @NotNull Vector3 pos2) {
        return Math.sqrt(
                pow(pos1.getX() - pos2.getX(), 2)
                        + pow(pos1.getY() - pos2.getY(), 2)
                        + pow(pos1.getZ() - pos2.getZ(), 2));
    }

    /**
     * pos1からpos4の値から生成されるベジエ曲線の曲線上の位置iの座標を求める。
     * @param pos pos1からpos4までの値
     * @param i 曲線上の位置(0 <= i <= 1)
     * @return 座標
     * @throws AssertionError p1からp4にnullが含まれる場合もしくはiが0以上1以下でない場合
     */
    public static Vector3 bezierCoordinate(Vector3 @NotNull [] pos, double i) {
        assert (pos[0] != null) && (pos[1] != null) && (pos[2] != null) && (pos[3] != null)
                : "値がnullのposがあります";
        assert (0 <= i) && (i <= 1)
                : "iの値が不正です";

        Vector3 p0 = pos[0];
        Vector3 p1 = pos[1];
        Vector3 p2 = pos[2];
        Vector3 p3 = pos[3];

        double x = pow(1-i, 3)*p0.getX() + 3*pow(1-i, 2)*i*p1.getX() + 3*(1-i)* pow(i, 2)*p2.getX() + pow(i, 3)*p3.getX() + 0.5;
        double y = pow(1-i, 3)*p0.getY() + 3*pow(1-i, 2)*i*p1.getY() + 3*(1-i)* pow(i, 2)*p2.getY() + pow(i, 3)*p3.getY() + 0.5;
        double z = pow(1-i, 3)*p0.getZ() + 3*pow(1-i, 2)*i*p1.getZ() + 3*(1-i)* pow(i, 2)*p2.getZ() + pow(i, 3)*p3.getZ() + 0.5;

        return Vector3.at(x, y, z);
    }

    public static Vector3 copyVector(Vector3 vec) {
        if (vec != null) return Vector3.at(vec.getX(), vec.getY(), vec.getZ());
        else return null;
    }
}