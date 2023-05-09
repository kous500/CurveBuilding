package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;

public final class BcCommand {
    public int n = 0;
    public int m = 0;
    public boolean air = false;
    public boolean isDirectionX = false;
    public boolean isDirectionZ = false;

    public boolean success = true;

    private final Player player;

    /**
     * bcコマンドの引数が正しいか調べ、引数の値をフィールドに代入する
     * @param args bcコマンドの引数
     * @param player コマンドを送信したプレイヤー
     */
    public BcCommand(String[] args, Player player) {
        this.player = player;

        int NumberCount = 0;
        String beforeArg = "";
        for (String arg : args) {
            if (arg.matches("-.*")) {
                if (arg.matches("^-[0-9]+$")) {
                    incorrectArgument(arg, beforeArg, NumberCount);
                    NumberCount++;
                } else {
                    boolean first = true;
                    boolean goodArg = true;

                    for(String s : arg.split("")) {
                        if (s.equals("-") && first) {
                            first = false;
                        } else if (s.equals("a") && !air) {
                            air = true;
                        } else if (s.equals("x") && !isDirectionX && !isDirectionZ) {
                            isDirectionX = true;
                        } else if (s.equals("z") && !isDirectionX && !isDirectionZ) {
                            isDirectionZ = true;
                        } else {
                            goodArg = false;
                        }
                    }

                    if (!goodArg) incorrectArgument(arg, beforeArg, NumberCount);
                }
            } else {
                if (NumberCount == 0 && incorrectArgument(arg, beforeArg, NumberCount)) {
                    n = Integer.parseInt(arg);
                } else if (NumberCount == 1 && incorrectArgument(arg, beforeArg, NumberCount)) {
                    m = Integer.parseInt(arg);
                } else if (NumberCount > 1) {
                    incorrectArgument(arg, beforeArg, NumberCount);
                }
                NumberCount++;
            }
            beforeArg = arg;
        }
    }

    private boolean incorrectArgument(String arg, String beforeArg, int NumberCount) {
        if (!arg.matches("^[0-9]+$") || NumberCount > 1) {
            if (NumberCount > 1) {
                player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
            } else if (arg.matches("^-[0-9]+$")) {
                player.printError(TextComponent.of(getMessage("messages.integer-less", 0, arg)));
            } else if (arg.matches("[+-]?\\d*(\\.\\d+)?")) {
                player.printError(TextComponent.of(getMessage("messages.invalid-integer", arg)));
            } else {
                player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
            }

            if (beforeArg.equals("")) beforeArg = "bc";
            player.printInfo(TextComponent.of("§7..."+beforeArg+"§c §c§n"+arg+"§c§o" + getMessage("messages.problem-here")));

            success = false;
            return false;
        }
        else return true;
    }
}