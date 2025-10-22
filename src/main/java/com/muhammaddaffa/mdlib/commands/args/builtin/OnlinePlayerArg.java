package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class OnlinePlayerArg implements ArgumentType<Player> {

    @Override public String id() { return "<player>"; }

    @Override
    public Player parse(CommandSender s, TokenReader t) {
        String name = t.next();
        if (name == null) throw new ArgParseException("Expected " + id());
        Player p = Bukkit.getPlayer(name);
        if (p == null) throw new ArgParseException("Player '" + name + "' not found");
        return p;
    }

    @Override
    public List<String> suggestions(CommandSender s, String prefix) {
        String pref = prefix == null ? "" : prefix.toLowerCase();
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase().startsWith(pref))
                .toList();
    }

}
