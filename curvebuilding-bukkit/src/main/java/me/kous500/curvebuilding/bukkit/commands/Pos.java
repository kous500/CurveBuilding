package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.bukkit.CurveBuildingPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.math.PosData.directionOptions;
import static me.kous500.curvebuilding.commands.pos.PosCommand.posCommand;

public class Pos implements TabExecutor {
    private final CurveBuildingPlugin plugin;

    public Pos(CurveBuildingPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            return posCommand(adapt((Player) sender), command.getName(), args);
        } else {
            plugin.getLogger().info(getMessage("messages.non-player-execution"));
            return true;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String[] types = new String[] {"f", "b"};
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("/bcpos")) {
            for (int i = 0; i < args.length; i++) {
                completions = new ArrayList<>();
                List<String> commands = new ArrayList<>();

                if (i == 0) {
                    commands.add("clear");
                    commands.add("clearall");
                    commands.add("insert");
                    commands.add("remove");
                    commands.add("set");
                    commands.add("shift");
                } else if (i == 1) {
                    if (args[0].equals("clear") || args[0].equals("set")) {
                        posComplete(args[i], commands, types);
                    } else if (args[0].equals("insert") || args[0].equals("remove")) {
                        posComplete(args[i], commands, new String[]{});
                    } else if (args[0].equals("shift") && !args[1].isEmpty() && args[1].matches("^[0-9]*")) {
                        for (String option : directionOptions) {
                            commands.add(args[1] + " " + option);
                        }
                    }
                }

                StringUtil.copyPartialMatches(args[i], commands, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }

    private void posComplete(String arg, List<String> commands, String[] types) {
        if (arg.matches("^[1-9][0-9]*$")) {
            for (String t : types) {
                if (!(arg + t).equals("1f")) commands.add(arg + t);
            }

            if (types.length == 0) {
                for (int i = 0; i <= 9; i++) {
                    commands.add(arg + i);
                }
            }
        } else if (arg.isEmpty()) {
            for (int i = 1; i <= 9; i++) {
                commands.add(arg + i);
            }
        }
    }
}