package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.Chat;
import me.kous500.curvebuilding.config.Message;

public final class BcCommand {
    /** Option letters after {@code -}: replace only air blocks. */
    public static final String AIR_OPTION = "a";
    /** Option letters after {@code -}: read the selection along x or along z; at most one of them. */
    public static final String[] DIRECTION_OPTIONS = {"x", "z"};

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
                        } else if (s.equals(AIR_OPTION) && !air) {
                            air = true;
                        } else if (s.equals(DIRECTION_OPTIONS[0]) && !isDirectionX && !isDirectionZ) {
                            isDirectionX = true;
                        } else if (s.equals(DIRECTION_OPTIONS[1]) && !isDirectionX && !isDirectionZ) {
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
        if (arg.matches("^[0-9]+$") && NumberCount <= 1) {
            try {
                Integer.parseInt(arg);
                return true;
            } catch (NumberFormatException e) {
                Chat.error(player, Message.INVALID_INTEGER, arg);
            }
        } else if (NumberCount > 1) {
            Chat.error(player, Message.INCORRECT_ARGUMENT);
        } else if (arg.matches("^-[0-9]+$")) {
            Chat.error(player, Message.INTEGER_LESS, 0, arg);
        } else if (arg.matches("[+-]?\\d*(\\.\\d+)?")) {
            Chat.error(player, Message.INVALID_INTEGER, arg);
        } else {
            Chat.error(player, Message.INCORRECT_ARGUMENT);
        }

        Chat.problemHere(player, beforeArg.isEmpty() ? "bc" : beforeArg, arg);

        success = false;
        return false;
    }
}