package me.kous500.curvebuilding;

import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.formatting.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTest {
    @Test
    void problemHereLooksLikeTheGamesCommandErrors() {
        List<Component> parts = Chat.problemHereText("set", "0").children();
        assertEquals(4, parts.size(), String.valueOf(parts));

        TextComponent before = assertInstanceOf(TextComponent.class, parts.get(0));
        assertEquals("...set", before.content());
        assertEquals(TextColor.GRAY, before.color());

        TextComponent arg = assertInstanceOf(TextComponent.class, parts.get(2));
        assertEquals("0", arg.content());
        assertEquals(TextColor.RED, arg.color());
        assertTrue(arg.hasDecoration(TextDecoration.UNDERLINED));

        TranslatableComponent here = assertInstanceOf(TranslatableComponent.class, parts.get(3));
        assertEquals("command.context.here", here.key());
        assertEquals(TextColor.RED, here.color());
        assertTrue(here.hasDecoration(TextDecoration.ITALIC));
        assertFalse(here.hasDecoration(TextDecoration.UNDERLINED));
    }

    @Test
    void gameMessageHasItsKeyAndArguments() {
        TranslatableComponent text = GameMessage.INTEGER_TOO_LOW.text(1, "-5");

        assertEquals("argument.integer.low", text.key());
        assertEquals(List.of("1", "-5"), text.args().stream()
                .map(arg -> assertInstanceOf(TextComponent.class, arg).content())
                .toList());
    }
}
