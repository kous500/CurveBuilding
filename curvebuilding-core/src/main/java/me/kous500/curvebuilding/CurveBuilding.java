package me.kous500.curvebuilding;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.Resources;

public class CurveBuilding {
    public static boolean fawe = false;
    public static Config config;
    public static Console console;
    private static Resources resources;

    public static void setResources(Resources resources) {
        CurveBuilding.resources = resources;
        config = resources.getConfig();
    }

    public static void setConsole(Console console) {
        CurveBuilding.console = console;
    }

    /**
     * 要求された文字列をパスで取得。
     * {0}から順番に引数の値が挿入されます。
     *
     * @param path 文字列を取得するためのパス
     * @param args 文字列に挿入する値
     * @return 結果の文字列
     */
    public static String getMessage(String path, Object... args) {
        return resources != null ? resources.getMessage(path, args) : path;
    }
}
