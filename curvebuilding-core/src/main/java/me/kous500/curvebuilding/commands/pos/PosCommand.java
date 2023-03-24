package me.kous500.curvebuilding.commands.pos;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import static me.kous500.curvebuilding.CurveBuilding.getMessage;
import static me.kous500.curvebuilding.PosData.addPos;
import static me.kous500.curvebuilding.PosData.clearPos;

public class PosCommand {
    public static boolean posCommand(Player player, @NotNull String command, @NotNull String[] args) {
        if (command.equalsIgnoreCase("/pos")) {
            if (args.length >= 2 && args[0].equals("clear")) {
                StringBuilder n = new StringBuilder();

                for(String s : args[1].split("")) {
                    if (s.matches("^[0-9]$")) {
                        n.append(s);
                    } else if (s.matches("[fb]")) {
                        String posN = n.toString();
                        if(!posN.equals("") && Integer.parseInt(posN) > 0) {
                            if (s.equals("f")) {
                                clearPos(player, Integer.parseInt(posN), 1);
                            } else if (s.equals("b")) {
                                clearPos(player, Integer.parseInt(posN), 2);
                            }

                            return true;
                        } else {
                            player.printError(TextComponent.of(getMessage("messages.integer-less", 1, posN)));
                            problemHere(player, args[0], args[1]);
                            return false;
                        }
                    } else {
                        player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                        problemHere(player, args[0], args[1]);
                        return false;
                    }
                }

                String posN = n.toString();
                if(!posN.equals("") && Integer.parseInt(posN) > 0) {
                    clearPos(player, Integer.parseInt(posN), 0);
                    return true;
                } else {
                    player.printError(TextComponent.of(getMessage("messages.integer-less", 1, posN)));
                    problemHere(player, args[0], args[1]);
                    return false;
                }
            } else if (args.length >= 1 && args[0].equals("clearall")) {
                clearPos(player);
                return true;
            } else if (args.length >= 1 && args[0].equals("insert")) {
                player.printInfo(TextComponent.of("'insert' is not yet implemented."));
                return true;
            } else if (args.length >= 2 && args[0].equals("set")) {
                StringBuilder n = new StringBuilder();
                for(String s : args[1].split("")) {
                    if (s.matches("^[0-9]$")) {
                        n.append(s);
                    } else if (s.matches("[fb]")) {
                        String posN = n.toString();
                        if(!posN.equals("") && Integer.parseInt(posN) > 0) {
                            if (s.equals("f")) {
                                addPos(player, Integer.parseInt(posN), 1);
                            } else if (s.equals("b")) {
                                addPos(player, Integer.parseInt(posN), 2);
                            }
                            return true;
                        } else {
                            player.printError(TextComponent.of(getMessage("messages.integer-less", 1, posN)));
                            problemHere(player, args[0], args[1]);
                            return false;
                        }
                    } else {
                        player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                        problemHere(player, args[0], args[1]);
                        return false;
                    }
                }

                String posN = n.toString();
                if(!posN.equals("") && Integer.parseInt(posN) > 0) {
                    addPos(player, Integer.parseInt(posN), 0);
                    return true;
                } else {
                    player.printError(TextComponent.of(getMessage("messages.integer-less", 1, posN)));
                    problemHere(player, args[0], args[1]);
                    return false;
                }
            } else {
                player.printError(TextComponent.of(getMessage("messages.incorrect-argument")));
                if (args.length >= 1) problemHere(player, "pos", args[0]);
                else problemHere(player, "pos", "");
                return false;
            }
        }

        return false;
    }

    public static void problemHere(@NotNull Player player, String beforeArg, String arg) {
        player.printInfo(TextComponent.of(
                "\u00a77..."+beforeArg+"\u00a7c \u00a7c\u00a7n"+arg+"\u00a7c\u00a7o" + getMessage("messages.problem-here")));
    }
}
