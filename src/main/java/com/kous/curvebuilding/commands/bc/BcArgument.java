package com.kous.curvebuilding.commands.bc;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class BcArgument {
    public int n = 0;
    public int m = 0;
    public boolean line = false;
    public boolean air = false;
    public boolean rtm = false;
    public boolean isDirectionX = false;
    public boolean isDirectionZ = false;

    public  boolean success = true;

    /**
     * bcコマンドの引数が正しいか調べ、引数の値をフィールドに代入する
     * @param args bcコマンドの引数
     * @param player コマンドを送信したプレイヤー
     */
    public BcArgument(String @NotNull [] args, Player player) {
        int NumberCount = 0;
        String beforeArg = "";
        for (String arg : args) {
            if (arg.matches("-.*")) {
                if (arg.matches("^-[0-9]+$")) {
                    badArgument(arg, beforeArg, player, NumberCount);
                    NumberCount++;
                } else {
                    boolean first = true;
                    boolean goodArg = true;

                    for(String s : arg.split("")) {
                        if (s.equals("-") && first) {
                            first = false;
                        } else if (s.equals("l") && !line) {
                            line = true;
                        } else if (s.equals("a") && !air) {
                            air = true;
                        } else if (s.equals("r") && !rtm) {
                            rtm = true;
                        } else if (s.equals("x") && !isDirectionX && !isDirectionZ) {
                            isDirectionX = true;
                        } else if (s.equals("z") && !isDirectionX && !isDirectionZ) {
                            isDirectionZ = true;
                        } else {
                            goodArg = false;
                        }
                    }

                    if (!goodArg) badArgument(arg, beforeArg, player, NumberCount);
                }
            } else {
                if (NumberCount == 0 && badArgument(arg, beforeArg, player, NumberCount)) {
                    n = Integer.parseInt(arg);
                } else if (NumberCount == 1 && badArgument(arg, beforeArg, player, NumberCount)) {
                    m = Integer.parseInt(arg);
                } else if (NumberCount > 1) {
                    badArgument(arg, beforeArg, player, NumberCount);
                }
                NumberCount++;
            }
            beforeArg = arg;
        }
    }

    private boolean badArgument(@NotNull String arg, String beforeArg, Player player, int NumberCount) {
        if (!arg.matches("^[0-9]+$") || NumberCount > 1) {
            if (NumberCount > 1) {
                player.sendMessage("\u00A7cコマンドの引数が正しくありません");
            } else if (arg.matches("^-[0-9]+$")) {
                player.sendMessage("\u00A7cこの整数は0以上でなくてはならないため、"+arg+"は適しません");
            } else if (arg.matches("[+-]?\\d*(\\.\\d+)?")) {
                player.sendMessage("\u00A7c「"+arg+"」は無効な整数です");
            } else {
                player.sendMessage("\u00A7cコマンドの引数が正しくありません");
            }

            if (beforeArg.equals("")) beforeArg = "bc";
            player.sendMessage("\u00a77..."+beforeArg+"\u00a7c \u00a7c\u00a7n"+arg+"\u00a7c\u00a7o←［問題箇所］");

            success = false;
            return false;
        }
        else return true;
    }
}