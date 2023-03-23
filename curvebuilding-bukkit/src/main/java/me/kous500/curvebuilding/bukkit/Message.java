package me.kous500.curvebuilding.bukkit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.bukkit.CurveBuildingPlugin.config;

public class Message {
    final private Plugin plugin;

    public Message(final Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        for (String language : new String[]{"en", "ja"}) {
            create("%datafolder%/messages/" + language + ".yml");
        }

        final File dataFolder = plugin.getDataFolder();
        final File file = new File(config.messageFilePath.replace("%datafolder%", dataFolder.toPath().toString()));

        if (file.exists())
            messages = YamlConfiguration.loadConfiguration(file);
        else messages = new YamlConfiguration();
    }

    public void create(String file) {
        try {
            final File dataFolder = plugin.getDataFolder();

            file = file.replace("%datafolder%", dataFolder.toPath().toString());

            final File configFile = new File(file);

            if (!configFile.exists()) {
                final String[] files = file.split("/");
                final InputStream inputStream = plugin.getClass().getClassLoader().getResourceAsStream("messages/" + files[files.length - 1]);
                final File parentFile = configFile.getParentFile();

                if (parentFile != null) parentFile.mkdirs();

                if (inputStream != null) {
                    Files.copy(inputStream, configFile.toPath());
                    plugin.getLogger().info("File " + configFile + " has been created!");
                } else configFile.createNewFile();
            }
        } catch (final IOException e) {
            plugin.getLogger().warning("Unable to create configuration file!");
        }
    }

    private static YamlConfiguration messages;

    /**
     * 要求された文字列をパスで取得。
     * {0}から順番に引数の値が挿入されます。
     *
     * @param path 文字列を取得するためのパス
     * @param args 文字列に挿入する値
     * @return 結果の文字列
     */
    public static String getMessage(@NotNull String path, Object @NotNull ... args) {
        String message = messages.getString(path, path);

        int i = 0;
        for (Object arg: args) {
            message = message.replace("{" + i + "}", arg.toString());
            i++;
        }

        return message;
    }

    /**
     * 指定のメッセージを赤色にして送信する。
     *
     * @param player メッセージを送信するプレイヤー
     * @param message 送信するメッセージ
     */
    public static void sendErrorMessage(@NotNull Player player, @NotNull String message) {
        player.sendMessage("\u00A7c" + message);
    }

    /**
     * 指定のメッセージを赤色にして送信する。
     *
     * @param player メッセージを送信するプレイヤー
     * @param message 送信するメッセージ
     */
    public static void sendErrorMessage(@NotNull com.sk89q.worldedit.entity.Player player, @NotNull String message) {
        sendErrorMessage(adapt(player), message);
    }
}