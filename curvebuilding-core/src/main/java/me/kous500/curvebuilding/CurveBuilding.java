package me.kous500.curvebuilding;

import org.jetbrains.annotations.NotNull;

public class CurveBuilding {
    private static Resources resources;

    public static Config config;

    public static void setResources(@NotNull Resources resources) {
        CurveBuilding.resources = resources;
        config = resources.getConfig();
    }

    /**
     * 要求された文字列をパスで取得。
     * {0}から順番に引数の値が挿入されます。
     *
     * @param path 文字列を取得するためのパス
     * @param args 文字列に挿入する値
     * @return 結果の文字列
     */
    public static String getMessage(@NotNull String path, Object @NotNull ... args) {
        return resources != null ? resources.getMessage(path, args) : path;
    }
}
