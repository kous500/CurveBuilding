package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.YamlConfig;

import java.awt.*;

public class FabricConfig extends Config {
    public boolean posRenderThroughWalls;
    public Color posLineColor;
    public Color posFilledColor;
    public Color posStartLineColor;
    public Color posStartFilledColor;
    public Color posEndLineColor;
    public Color posEndFilledColor;
    public Color posFLineColor;
    public Color posFFilledColor;
    public Color posBLineColor;
    public Color posBFilledColor;

    public boolean lineRenderThroughWalls;
    public int lineRenderAccuracy;
    public int lineRenderLength;
    public Color lineFColor;
    public Color lineBColor;
    public Color lineCurveColor;

    public FabricConfig(YamlConfig config) {
        messageFilePath = config.getString("message-file");

        fineness = config.getInteger("curve.fineness", 1);
        maxSetLength = config.getInteger("curve.max-set-length", 0);
        tCenter = config.getBoolean("curve.thicken-center");
        defaultMaxChangeLimit = config.getInteger("curve.default-max-change-limit", -1);

        posRenderThroughWalls = config.getBoolean("render-preview.pos.render-through-walls");
        posLineColor = new Color(config.getInteger("render-preview.pos.format.pos.line-color"), true);
        posFilledColor = new Color(config.getInteger("render-preview.pos.format.pos.filled-color"), true);
        posStartLineColor = new Color(config.getInteger("render-preview.pos.format.start-pos.line-color"), true);
        posStartFilledColor = new Color(config.getInteger("render-preview.pos.format.start-pos.filled-color"), true);
        posEndLineColor = new Color(config.getInteger("render-preview.pos.format.end-pos.line-color"), true);
        posEndFilledColor = new Color(config.getInteger("render-preview.pos.format.end-pos.filled-color"), true);
        posFLineColor = new Color(config.getInteger("render-preview.pos.format.f.line-color"), true);
        posFFilledColor = new Color(config.getInteger("render-preview.pos.format.f.filled-color"), true);
        posBLineColor = new Color(config.getInteger("render-preview.pos.format.b.line-color"), true);
        posBFilledColor = new Color(config.getInteger("render-preview.pos.format.b.filled-color"), true);

        lineRenderThroughWalls = config.getBoolean("render-preview.line.render-through-walls");
        lineRenderAccuracy = config.getInteger("render-preview.line.accuracy", 1);
        lineRenderLength = config.getInteger("render-preview.line.maxLength", 0);
        lineFColor = new Color(config.getInteger("render-preview.line.format.line-f-color"), true);
        lineBColor = new Color(config.getInteger("render-preview.line.format.line-b-color"), true);
        lineCurveColor = new Color(config.getInteger("render-preview.line.format.curve-color"), true);
    }
}
