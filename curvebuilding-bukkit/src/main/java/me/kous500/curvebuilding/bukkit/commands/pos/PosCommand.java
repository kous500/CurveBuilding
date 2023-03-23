package me.kous500.curvebuilding.bukkit.commands.pos;

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

import static me.kous500.curvebuilding.bukkit.Message.getMessage;
import static me.kous500.curvebuilding.bukkit.Message.sendErrorMessage;
import static me.kous500.curvebuilding.bukkit.Pos.*;
import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;

public class PosCommand implements TabExecutor {
    private final CurveBuildingPlugin plugin;

    public PosCommand(CurveBuildingPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player) {
            com.sk89q.worldedit.entity.Player player = adapt((Player) sender);

            if (command.getName().equalsIgnoreCase("/pos")) {
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
                                sendErrorMessage(player, getMessage("messages.integer-less", 1, posN));
                                problemHere(player, args[0], args[1]);
                                return false;
                            }
                        } else {
                            sendErrorMessage(player, getMessage("messages.incorrect-argument"));
                            problemHere(player, args[0], args[1]);
                            return false;
                        }
                    }

                    String posN = n.toString();
                    if(!posN.equals("") && Integer.parseInt(posN) > 0) {
                        clearPos(player, Integer.parseInt(posN), 0);
                        return true;
                    } else {
                        sendErrorMessage(player, getMessage("messages.integer-less", 1, posN));
                        problemHere(player, args[0], args[1]);
                        return false;
                    }
                } else if (args.length >= 1 && args[0].equals("clearall")) {
                    clearPos(player);
                    return true;
                } else if (args.length >= 1 && args[0].equals("insert")) {
                    sendErrorMessage(adapt(player), "'insert' is not yet implemented.");
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
                                sendErrorMessage(player, getMessage("messages.integer-less", 1, posN));
                                problemHere(player, args[0], args[1]);
                                return false;
                            }
                        } else {
                            sendErrorMessage(player, getMessage("messages.incorrect-argument"));
                            problemHere(player, args[0], args[1]);
                            return false;
                        }
                    }

                    String posN = n.toString();
                    if(!posN.equals("") && Integer.parseInt(posN) > 0) {
                        addPos(player, Integer.parseInt(posN), 0);
                        return true;
                    } else {
                        sendErrorMessage(player, getMessage("messages.integer-less", 1, posN));
                        problemHere(player, args[0], args[1]);
                        return false;
                    }
                } else {
                    sendErrorMessage(player, getMessage("messages.incorrect-argument"));
                    if (args.length >= 1) problemHere(player, "pos", args[0]);
                    else problemHere(player, "pos", "");
                    return false;
                }
            }
        } else {
            plugin.getLogger().info(getMessage("messages.non-player-execution"));
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String[] types = new String[] {"f", "b"};
        List<String> completions = new ArrayList<>();

        if (command.getName().equalsIgnoreCase("/pos")) {
            for (int i = 0; i < args.length; i++) {
                completions = new ArrayList<>();
                List<String> commands = new ArrayList<>();

                if (i == 0) {
                    commands.add("clear");
                    commands.add("clearall");
                    //commands.add("insert");
                    commands.add("set");
                } else if (i == 1) {
                    if (args[0].equals("clear") || args[0].equals("set")) {
                        posComplete(args[i], commands, types);
                    } else if (args[0].equals("insert")) {
                        posComplete(args[i], commands, new String[]{});
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
                commands.add(arg + t);
            }

            if (types.length == 0) {
                for (int i = 0; i <= 9; i++) {
                    commands.add(arg + i);
                }
            }
        } else if (arg.equals("")) {
            for (int i = 1; i <= 9; i++) {
                commands.add(arg + i);
            }
        }
    }

    private void problemHere(com.sk89q.worldedit.entity.Player player, String beforeArg, String arg) {
        adapt(player).sendMessage("\u00a77..."+beforeArg+"\u00a7c \u00a7c\u00a7n"+arg+"\u00a7c\u00a7o" + getMessage("messages.problem-here"));
    }
}