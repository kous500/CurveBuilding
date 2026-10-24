package me.kous500.curvebuilding.fabric.commands;

import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import static me.kous500.curvebuilding.fabric.CurveBuildingFabric.debugMode;

public interface RunCommand {
    int run();

    CommandSourceStack getSource();

    static int debugRun(RunCommand command) {
        if (!debugMode) return command.run();

        try {
            return command.run();
        } catch (Exception e) {
            CommandSourceStack source = command.getSource();

            source.sendSystemMessage(Component.literal(String.valueOf(e)));
            Arrays.stream(e.getStackTrace())
                    .map(stack -> Component.literal(String.valueOf(stack)))
                    .forEach(source::sendSystemMessage);

            throw e;
        }
    }
}
