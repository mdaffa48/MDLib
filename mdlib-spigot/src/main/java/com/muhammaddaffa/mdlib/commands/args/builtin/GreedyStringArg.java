package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.command.CommandSender;

public final class GreedyStringArg implements ArgumentType<String> {

    @Override public String id() { return "<text...>"; }
    @Override public boolean greedy() { return true; }

    @Override
    public String parse(CommandSender s, TokenReader t) {
        String rest = t.remainingJoined();
        if (rest.isEmpty()) throw new ArgParseException("Expected " + id());
        return rest;
    }

}
