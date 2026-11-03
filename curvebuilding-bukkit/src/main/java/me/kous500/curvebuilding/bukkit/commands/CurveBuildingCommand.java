package me.kous500.curvebuilding.bukkit.commands;

import me.kous500.curvebuilding.message.Chat;
import me.kous500.curvebuilding.message.GameMessage;
import me.kous500.curvebuilding.message.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static me.kous500.curvebuilding.message.Messages.getMessage;

/**
 * The command {@code /curvebuilding}, which reloads the files with {@code reload}.
 */
public class CurveBuildingCommand implements TabExecutor {
    private final Runnable reload;

    /**
     * The command, which runs {@code reload} for {@code /curvebuilding reload}.
     *
     * @param reload reads the files again
     */
    public CurveBuildingCommand(Runnable reload) {
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
            reload.run();
            if (sender instanceof Player player) {
                Chat.info(adapt(player), Message.RELOADED);
            } else {
                sender.sendMessage(getMessage(Message.RELOADED));
            }
            return true;
        } else {
            if (sender instanceof Player player) {
                Chat.error(adapt(player), GameMessage.INCORRECT_ARGUMENT);
            } else {
                sender.sendMessage(getMessage(Message.INCORRECT_ARGUMENT));
            }
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Collections.singletonList("reload"), new ArrayList<>());
        }
        return Collections.emptyList();
    }
}
