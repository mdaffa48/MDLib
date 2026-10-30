package com.muhammaddaffa.mdlib.core.text;

import com.muhammaddaffa.mdlib.utils.ColorComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ColorsTest {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @AfterEach
    void removeTags() {
        Colors.clearTagResolvers();
    }

    @Test
    void preservesInteractiveTagsAndQuotedArguments() {
        String input = "<click:open_url:'https://example.com/?a=1&b=2#abcdef'>"
                + "<hover:show_text:'<red>Nested <bold>text</bold> > &a #abcdef'>Open</hover></click>";
        assertEquals(miniMessage.deserialize(input), Colors.component(input));
    }

    @Test
    void supportsLegacyColorsAlongsideMiniMessage() {
        assertEquals(miniMessage.deserialize("<reset><green>Hello <bold>world</bold>"),
                Colors.component("&aHello <bold>world</bold>"));
        assertEquals(Colors.component("&#12ab34Hex"), Colors.component("§x§1§2§a§b§3§4Hex"));
    }

    @Test
    void everyLegacyFormatCodeMapsToItsMiniMessageTag() {
        assertEquals(miniMessage.deserialize("<obfuscated>a<bold>b<strikethrough>c<underlined>d<italic>e<reset>f"),
                Colors.component("&ka&lb&mc&nd&oe&rf"));
        assertEquals(miniMessage.deserialize("<reset><black>0<reset><dark_blue>1<reset><gold>6<reset><light_purple>d"),
                Colors.component("&00&11&66&dd"));
        assertEquals(miniMessage.deserialize("<reset><red>upper"), Colors.component("&Cupper"));
        String[] names = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray",
                "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
        String codes = "0123456789abcdef";
        for (int i = 0; i < codes.length(); i++) {
            assertEquals(miniMessage.deserialize("<reset><" + names[i] + ">x"), Colors.component("&" + codes.charAt(i) + "x"),
                    "&" + codes.charAt(i));
        }
    }

    @Test
    void legacyOutputUsesSectionCodesAndRepeatedHex() {
        assertEquals("§aHi", Colors.color("&aHi"));
        assertEquals("§x§1§2§a§b§3§4Hex", Colors.color("&#12ab34Hex"));
        assertEquals("§cRed", Colors.color("<red>Red"));
        assertEquals(List.of("§aA", "§bB"), Colors.color(List.of("&aA", "&bB")));
    }

    @Test
    void globalAndPerMessageResolversPreserveComponents() {
        Component button = Component.text("Click").clickEvent(ClickEvent.runCommand("/help"));
        Colors.registerTagResolver("test", Placeholder.component("button", button));
        assertEquals(button, Colors.component("<button>"));
        assertEquals(Component.text("Override"), Colors.component("<button>",
                Placeholder.component("button", Component.text("Override"))));
        Colors.unregisterTagResolver("test");
        assertEquals(Component.text("<button>"), Colors.component("<button>"));
    }

    @Test
    void colorComponentUsesTheSharedResolversAndTurnsItalicsOff() {
        Colors.registerTagResolver("test", Placeholder.parsed("name", "Steve"));
        assertEquals(Component.text("Steve").decoration(TextDecoration.ITALIC, false),
                ColorComponent.colorToComponent("<name>"));
        String input = "<italic><click:run_command:'/help'>Help</click></italic>";
        assertEquals(miniMessage.deserialize(input), ColorComponent.colorToComponent(input));
    }

    @Test
    void nullRemainsSafe() {
        assertEquals(Component.empty(), Colors.component((String) null));
        assertNull(Colors.color((String) null));
    }
}
