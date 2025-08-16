package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.config.Config;
import me.kous500.curvebuilding.config.YamlConfig;

import java.awt.Color;

public final class FabricConfig extends Config {
    public final boolean posRenderThroughWalls;
    public final Color posLineColor;
    public final Color posFilledColor;
    public final Color posStartLineColor;
    public final Color posStartFilledColor;
    public final Color posEndLineColor;
    public final Color posEndFilledColor;
    public final Color posFLineColor;
    public final Color posFFilledColor;
    public final Color posBLineColor;
    public final Color posBFilledColor;

    public final boolean lineRenderThroughWalls;
    public final int lineRenderAccuracy;
    public final int lineRenderLength;
    public final Color lineFColor;
    public final Color lineBColor;
    public final Color lineCurveColor;

    public FabricConfig(YamlConfig config) {
        messageFilePath = config.getString("message-file");

        fineness = Math.max(1, config.getInteger("curve.fineness"));
        maxSetLength = Math.max(0, config.getInteger("curve.max-set-length"));
        maxPosValue = Math.max(1, config.getInteger("curve.max-pos-value"));
        tCenter = config.getBoolean("curve.thicken-center");
        defaultMaxChangeLimit = Math.max(-1, config.getInteger("curve.default-max-change-limit"));

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
        lineRenderAccuracy = Math.max(1, config.getInteger("render-preview.line.accuracy"));
        lineRenderLength = Math.max(0, config.getInteger("render-preview.line.maxLength"));
        lineFColor = new Color(config.getInteger("render-preview.line.format.line-f-color"), true);
        lineBColor = new Color(config.getInteger("render-preview.line.format.line-b-color"), true);
        lineCurveColor = new Color(config.getInteger("render-preview.line.format.curve-color"), true);
    }
}