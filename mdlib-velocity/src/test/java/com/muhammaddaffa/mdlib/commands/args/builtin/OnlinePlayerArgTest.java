package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnlinePlayerArgTest {

    @TempDir
    Path directory;
    private final OnlinePlayerArg arg = new OnlinePlayerArg();
    private final CommandSource sender = mock(CommandSource.class);
    private Player steve;

    @BeforeEach
    void setUp() {
        ProxyServer server = mock(ProxyServer.class);
        steve = player("Steve");
        List<Player> online = List.of(steve, player("STEVO"), player("alex"));
        when(server.getCommandManager()).thenReturn(mock(CommandManager.class));
        when(server.getPlayer("Steve")).thenReturn(Optional.of(steve));
        when(server.getAllPlayers()).thenReturn(online);
        MDLib.onEnable(new Object(), server, mock(org.slf4j.Logger.class), directory);
    }

    @AfterEach
    void tearDown() {
        MDLib.shutdown();
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getUsername()).thenReturn(name);
        return player;
    }

    @Test
    void parsesAnOnlinePlayer() {
        assertSame(steve, arg.parse(sender, new TokenReader(new String[]{"Steve"})));
    }

    @Test
    void unknownOrMissingNameThrows() {
        ArgParseException unknown = assertThrows(ArgParseException.class,
                () -> arg.parse(sender, new TokenReader(new String[]{"Herobrine"})));
        ArgParseException missing = assertThrows(ArgParseException.class,
                () -> arg.parse(sender, new TokenReader(new String[0])));

        assertEquals("Player 'Herobrine' not found", unknown.getMessage());
        assertEquals("Expected <player>", missing.getMessage());
    }

    @Test
    void suggestionsMatchPrefixIgnoringCase() {
        assertEquals(List.of("Steve", "STEVO"), arg.suggestions(sender, "st"));
        assertEquals(List.of("alex"), arg.suggestions(sender, "AL"));
        assertEquals(List.of("Steve", "STEVO", "alex"), arg.suggestions(sender, null));
    }

}
