package me.kous500.curvebuilding.bukkit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static me.kous500.curvebuilding.CurveBuilding.config;

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

        BukkitResources.messages = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
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
}