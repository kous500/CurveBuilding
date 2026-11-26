package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.entity.Player;
import me.kous500.curvebuilding.config.Config;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BcCommandTest {
    private final List<String> calls = new ArrayList<>();
    private final List<EditRunner.Edit> edits = new ArrayList<>();
    private final Player player = player();

    /** A player that records the methods called on it, other than those of {@link Object}. */
    private Player player() {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "toString" -> "player";
                    default -> {
                        calls.add(method.getName());
                        yield method.getName().equals("getUniqueId") ? id : null;
                    }
                }
        );
    }

    private boolean run(String... args) {
        return BcCommand.bcCommand(player, args, new Config(), (runPlayer, edit) -> edits.add(edit));
    }

    @Test
    void theRunnerGetsTheEditWithoutItRunning() {
        assertTrue(run("-a", "5"));

        assertEquals(1, edits.size());
        assertEquals(List.of("getUniqueId"), calls);
    }

    @Test
    void theRunnerIsNotCalledForAnArgumentThatCannotBeUsed() {
        assertFalse(run("x"));

        assertTrue(edits.isEmpty());
        assertTrue(calls.contains("printError"), String.valueOf(calls));
    }
}
