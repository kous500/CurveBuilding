package me.kous500.curvebuilding;

import org.jetbrains.annotations.NotNull;

public interface Resources {
    String getMessage(@NotNull String path, Object @NotNull ... args);

    Config getConfig();
}
