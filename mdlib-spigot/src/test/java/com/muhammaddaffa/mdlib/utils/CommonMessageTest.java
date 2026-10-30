package com.muhammaddaffa.mdlib.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonMessageTest {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @AfterEach
    void removeTags() {
        Common.unregisterTagResolver("test");
    }

    @Test
    void preservesInteractiveTagsAndQuotedArguments() {
        String input = "<click:open_url:'https://example.com/?a=1&b=2#abcdef'>"
                + "<hover:show_text:'<red>Nested <bold>text</bold> > &a #abcdef'>Open</hover></click>";
        assertEquals(miniMessage.deserialize(input), Common.component(input));
    }

    @Test
    void preservesEscapesAndOtherStandardTags() {
        String input = "\\<red>literal <hover:show_text:'It\\'s <blue>blue'>hover</hover> "
                + "<gradient:red:blue>gradient</gradient><newline><key:key.jump><lang:block.minecraft.stone>";
        assertEquals(GsonComponentSerializer.gson().serialize(miniMessage.deserialize(input)),
                GsonComponentSerializer.gson().serialize(Common.component(input)));
    }

    @Test
    void supportsLegacyColorsAlongsideMiniMessage() {
        assertEquals(miniMessage.deserialize("<reset><green>Hello <bold>world</bold>"),
                Common.component("&aHello <bold>world</bold>"));
        assertEquals(Common.component("&#12ab34Hex"), Common.component("§x§1§2§a§b§3§4Hex"));
    }

    @Test
    void globalAndPerMessageResolversPreserveComponents() {
        Component button = Component.text("Click").clickEvent(
                net.kyori.adventure.text.event.ClickEvent.runCommand("/help"));
        Common.registerTagResolver("test", Placeholder.component("button", button));
        assertEquals(button, Common.component("<button>"));
        assertEquals(Component.text("Override"), Common.component("<button>",
                Placeholder.component("button", Component.text("Override"))));
        Common.unregisterTagResolver("test");
        assertEquals(Component.text("<button>"), Common.component("<button>"));
    }

    @Test
    void colorComponentKeepsExplicitItalicsAndEvents() {
        String input = "<italic><click:run_command:'/help'>Help</click></italic>";
        assertEquals(miniMessage.deserialize(input), ColorComponent.colorToComponent(input));
        assertEquals(TextDecoration.State.FALSE,
                ColorComponent.colorToComponent("Plain").decoration(TextDecoration.ITALIC));
    }

    @Test
    void nullRemainsSafe() {
        assertEquals(Component.empty(), Common.component((String) null));
        assertNull(Common.color((String) null));
    }
}
