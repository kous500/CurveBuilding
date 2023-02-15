package com.kous.curvebuilding.commands.bc;

import com.kous.curvebuilding.CurveBuilding;
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

import static com.kous.curvebuilding.Message.getMessage;

public class BcCommand implements TabExecutor {
    private final CurveBuilding plugin;

    private final String[] option = {"l", "a", "r"};
    private final String[] directionOption = {"x", "z"};

    public BcCommand(CurveBuilding plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //コマンドの名前がtestであるかを確かめる
        if (command.getName().equalsIgnoreCase("/bc")) {
            if (sender instanceof Player) {
                // プレイヤーが実行した場合の処理
                BcArgument argument = new BcArgument(args, (Player) sender);
                if (argument.success) {
                    new BcEdit((Player) sender, argument);
                    return true;
                }
            } else {
                // サーバーが実行した場合の処理
                plugin.getLogger().info(getMessage("messages.non-player-execution", sender.getName()));
                return true;
            }
        }
        //コマンドが存在しない場合はfalseを返す
        return false;
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String @NotNull [] args) {
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