package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.List;

public final class OnlinePlayerArg implements ArgumentType<Player> {

    @Override public String id() { return "<player>"; }

    @Override
    public Player parse(CommandSource s, TokenReader t) {
        String name = t.next();
        if (name == null) throw new ArgParseException("Expected " + id());
        return MDLib.server().getPlayer(name)
                .orElseThrow(() -> new ArgParseException("Player '" + name + "' not found"));
    }

    @Override
    public List<String> suggestions(CommandSource s, String prefix) {
        String pref = prefix == null ? "" : prefix.toLowerCase();
        return MDLib.server().getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(n -> n.toLowerCase().startsWith(pref))
                .toList();
    }

}
