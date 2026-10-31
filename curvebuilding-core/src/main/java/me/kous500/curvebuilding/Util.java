package me.kous500.curvebuilding;

import static java.lang.Math.*;

/**
 * このプラグインで共通で使う処理をまとめています。
 */
public final class Util {

    private Util() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
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
}