package me.kous500.curvebuilding;

public interface Resources {
    String getMessage(String path, Object... args);

    Config getConfig();
}
