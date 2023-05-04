package me.kous500.curvebuilding.fabric.config;

import me.kous500.curvebuilding.Config;

public class FabricConfig extends Config {
    public boolean posRenderThroughWalls;
    public int posLineColor;
    public int posFilledColor;
    public int posStartLineColor;
    public int posStartFilledColor;
    public int posEndLineColor;
    public int posEndFilledColor;
    public int posFLineColor;
    public int posFFilledColor;
    public int posBLineColor;
    public int posBFilledColor;

    public boolean lineRenderThroughWalls;
    public int lineRenderAccuracy;
    public int lineRenderLength;
    public int lineFColor;
    public int lineBColor;
    public int lineCurveColor;

    public FabricConfig (YamlConfig config) {
        messageFilePath = config.getString("message-file");

        fineness = config.getInteger("curve.fineness", 1);
        maxSetLength = config.getInteger("curve.max-set-length", 0);
        tCenter = config.getBoolean("curve.thicken-center");
        defaultMaxChangeLimit = config.getInteger("curve.default-max-change-limit", 0);

        posRenderThroughWalls = config.getBoolean("render-preview.pos.render-through-walls");
        posLineColor = config.getInteger("render-preview.pos.format.pos.line-color");
        posFilledColor = config.getInteger("render-preview.pos.format.pos.filled-color");
        posStartLineColor = config.getInteger("render-preview.pos.format.start-pos.line-color");
        posStartFilledColor = config.getInteger("render-preview.pos.format.start-pos.filled-color");
        posEndLineColor = config.getInteger("render-preview.pos.format.end-pos.line-color");
        posEndFilledColor = config.getInteger("render-preview.pos.format.end-pos.filled-color");
        posFLineColor = config.getInteger("render-preview.pos.format.f.line-color");
        posFFilledColor = config.getInteger("render-preview.pos.format.f.filled-color");
        posBLineColor = config.getInteger("render-preview.pos.format.b.line-color");
        posBFilledColor = config.getInteger("render-preview.pos.format.b.filled-color");

        lineRenderThroughWalls = config.getBoolean("render-preview.line.render-through-walls");
        lineRenderAccuracy = config.getInteger("render-preview.line.accuracy");
        lineRenderLength = config.getInteger("render-preview.line.maxLength");
        lineFColor = config.getInteger("render-preview.line.format.line-f-color");
        lineBColor = config.getInteger("render-preview.line.format.line-b-color");
        lineCurveColor = config.getInteger("render-preview.line.format.curve-color");
    }
}
