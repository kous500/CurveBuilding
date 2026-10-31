package me.kous500.curvebuilding.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Command suggestions from a completion in core, which takes the arguments the way Bukkit passes them to tab
 * completion. A suggestion may hold a space, such as {@code 5 back}; it replaces the argument being typed.
 */
final class CoreSuggestions {
    private CoreSuggestions() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static SuggestionProvider<CommandSourceStack> of(Function<String[], List<String>> completion) {
        return (context, builder) -> {
            for (String suggestion : completion.apply(arguments(context, builder))) builder.suggest(suggestion);
            return builder.buildFuture();
        };
    }

    /** The arguments after the command name; the last is the one being typed, up to the cursor. */
    private static String[] arguments(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        int commandEnd = context.getNodes().get(0).getRange().getEnd();
        List<String> args = new ArrayList<>(Arrays.asList(
                CommandInput.split(builder.getInput().substring(commandEnd, builder.getStart()))));
        args.add(builder.getRemaining());
        return args.toArray(String[]::new);
    }
}
