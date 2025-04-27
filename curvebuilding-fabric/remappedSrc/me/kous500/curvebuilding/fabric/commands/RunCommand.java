package me.kous500.curvebuilding.fabric.commands;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Arrays;

import static me.kous500.curvebuilding.fabric.CurveBuildingFabric.debugMode;

public interface RunCommand {
    int run();

    ServerCommandSource getSource();

    static int debugRun(RunCommand command) {
        if (!debugMode) return command.run();

        try {
            return command.run();
        } catch (Exception e) {
            ServerCommandSource source = command.getSource();

            source.sendMessage(Text.literal(String.valueOf(e)));
            Arrays.stream(e.getStackTrace())
                    .map(stack -> Text.literal(String.valueOf(stack)))
                    .forEach(source::sendMessage);

            throw e;
        }
    }
}
