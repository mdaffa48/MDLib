package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;

public final class OnlinePlayerArg implements ArgumentType<ProxiedPlayer> {

    @Override public String id() { return "<player>"; }

    @Override
    public ProxiedPlayer parse(CommandSender s, TokenReader t) {
        String name = t.next();
        if (name == null) throw new ArgParseException("Expected " + id());
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(name);
        if (p == null) throw new ArgParseException("Player '" + name + "' not found");
        return p;
    }

    @Override
    public List<String> suggestions(CommandSender s, String prefix) {
        String pref = prefix == null ? "" : prefix.toLowerCase();
        return ProxyServer.getInstance().getPlayers().stream()
                .map(ProxiedPlayer::getName)
                .filter(n -> n.toLowerCase().startsWith(pref))
                .toList();
    }

}
