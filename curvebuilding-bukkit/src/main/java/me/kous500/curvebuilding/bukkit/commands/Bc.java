package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.bukkit.CurveBuildingPlugin;
import me.kous500.curvebuilding.commands.bc.BcCommand;
import me.kous500.curvebuilding.commands.bc.BcEdit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.CurveBuilding.getMessage;

public class Bc implements TabExecutor {
    private final CurveBuildingPlugin plugin;

    private final String[] option = {"a"};
    private final String[] directionOption = {"x", "z"};

    public Bc(CurveBuildingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("/bc")) {
            if (sender instanceof Player) {
                BcCommand argument = new BcCommand(args, adapt((Player) sender));
                if (argument.success) {
                    new BcEdit(adapt((Player) sender), argument);
                    return true;
                }
            } else {
                plugin.getLogger().info(getMessage("messages.non-player-execution"));
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        List<String> commands;

        boolean[] useOption = new boolean[option.length];
        Arrays.fill(useOption, false);
        boolean useDirectionOption = false;

        for (String arg : args) {
            completions = new ArrayList<>();
            commands = new ArrayList<>();
            boolean first = true;
            boolean goodArg = true;

            if (arg.matches("-.*")) {
                for(String s : arg.split("")) {
                    boolean isS_Option = false;

                    for (int i = 0; i < option.length; i++) {
                        if (s.equals(option[i]) && !useOption[i] && goodArg) {
                            useOption[i] = true;
                            isS_Option = true;
                        }
                    }
                    for (String o : directionOption) {
                        if (s.equals(o) && !useDirectionOption && goodArg) {
                            useDirectionOption = true;
                            isS_Option = true;
                            break;
                        }
                    }

                    if (first) {
                        isS_Option = true;
                        first = false;
                    }

                    if (!isS_Option) goodArg = false;
                }

                for (int i = 0; i < option.length; i++) {
                    if (!useOption[i] && goodArg) {
                        commands.add(arg + option[i]);
                    }
                }
                for (String o : directionOption) {
                    if (!useDirectionOption && goodArg) {
                        commands.add(arg + o);
                    }
                }

                StringUtil.copyPartialMatches(arg, commands, completions);
            }
        }

        Collections.sort(completions);
        return completions;
    }
}