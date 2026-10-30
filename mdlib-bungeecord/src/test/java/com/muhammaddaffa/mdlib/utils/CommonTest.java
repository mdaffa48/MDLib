package com.muhammaddaffa.mdlib.utils;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.Title;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CommonTest {

    private MockedStatic<ProxyServer> statics;
    private ProxyServer proxy;
    private ProxiedPlayer player;
    private CommandSender console;

    @BeforeEach
    void setUp() {
        proxy = mock(ProxyServer.class);
        player = mock(ProxiedPlayer.class);
        console = mock(CommandSender.class);
        when(proxy.getPlayers()).thenReturn(List.of(player));
        when(proxy.getConsole()).thenReturn(console);
        statics = mockStatic(ProxyServer.class);
        statics.when(ProxyServer::getInstance).thenReturn(proxy);
    }

    @AfterEach
    void tearDown() {
        statics.close();
    }

    private static List<String> chat(CommandSender sender) {
        ArgumentCaptor<BaseComponent> sent = ArgumentCaptor.forClass(BaseComponent.class);
        verify(sender, atLeast(0)).sendMessage(sent.capture());
        return sent.getAllValues().stream().map(component -> component.toPlainText()).toList();
    }

    private List<String> actionBar() {
        ArgumentCaptor<BaseComponent> sent = ArgumentCaptor.forClass(BaseComponent.class);
        verify(player, atLeast(0)).sendMessage(eq(ChatMessageType.ACTION_BAR), sent.capture());
        return sent.getAllValues().stream().map(component -> component.toPlainText()).toList();
    }

    @Test
    void actionbarPrefixGoesToThePlayersActionBarAndIsStrippedForTheConsole() {
        Placeholder placeholder = new Placeholder().add("{name}", "Steve");

        Common.sendMessage(player, "actionbar;&aHi {name}", placeholder);
        Common.sendMessage(console, "actionbar;Hello {name}", placeholder);
        Common.sendMessage(console, "");
        Common.sendMessage(console, (String) null);
        Common.sendMessage(player, List.of("one", "two {name}"), placeholder);
        Common.actionBar(player, "bar {name}", placeholder);

        assertEquals(List.of("Hi Steve", "bar Steve"), actionBar());
        assertEquals(List.of("Hello Steve"), chat(console));
        assertEquals(List.of("one", "two Steve"), chat(player));
    }

    @Test
    void legacyAndHexColorsReachTheBungeeComponent() {
        Common.sendMessage(console, "&#ff0000Red &lbold");

        ArgumentCaptor<BaseComponent> sent = ArgumentCaptor.forClass(BaseComponent.class);
        verify(console).sendMessage(sent.capture());
        assertEquals("Red bold", sent.getValue().toPlainText());
        String legacy = sent.getValue().toLegacyText();
        assertTrue(legacy.contains("§x§f§f§0§0§0§0Red"), legacy);
        assertTrue(legacy.contains("§lbold"), legacy);
    }

    @Test
    void broadcastReachesEveryPlayerAndTheConsole() {
        Common.broadcast("Hi %who%", new Placeholder().add("%who%", "all"));
        Common.broadcast(null);

        assertEquals(List.of("Hi all", ""), chat(player));
        assertEquals(List.of("Hi all", ""), chat(console));
    }

    @Test
    void titlesAreBuiltWithTickTimes() {
        Title title = mock(Title.class, RETURNS_SELF);
        when(proxy.createTitle()).thenReturn(title);

        Common.sendTitle(player, "&aTitle {x}", "Sub {x}", new Placeholder().add("{x}", "1"));
        Common.sendTitle(player, "a", "b", 1, 2, 3);

        ArgumentCaptor<BaseComponent> titles = ArgumentCaptor.forClass(BaseComponent.class);
        ArgumentCaptor<BaseComponent> subtitles = ArgumentCaptor.forClass(BaseComponent.class);
        verify(title, times(2)).title(titles.capture());
        verify(title, times(2)).subTitle(subtitles.capture());
        assertEquals(List.of("Title 1", "a"), titles.getAllValues().stream().map(component -> component.toPlainText()).toList());
        assertEquals(List.of("Sub 1", "b"), subtitles.getAllValues().stream().map(component -> component.toPlainText()).toList());
        verify(title).fadeIn(20);
        verify(title).stay(40);
        verify(title).fadeOut(20);
        verify(title).fadeIn(1);
        verify(title).stay(2);
        verify(title).fadeOut(3);
        verify(title, times(2)).send(player);
        verify(player, never()).sendMessage(any(BaseComponent.class));
    }

    @Test
    void wordUtilsMatchSpigot() {
        assertEquals("Armor Stand", WordUtils.getNiceName("ARMOR_STAND"));
        assertEquals("Hello", WordUtils.upperCaseFirstLetterOnly("hELLO"));
        assertEquals("b c", WordUtils.buildString(new String[]{"a", "b", "c"}, 1));
        assertEquals(2, WordUtils.countChar("a.b.c", '.'));
        assertEquals("'", WordUtils.getGenitiveSuffix("Jesu§cs"));
        assertEquals("'s", WordUtils.getGenitiveSuffix("mfnalex§c"));
    }

}
