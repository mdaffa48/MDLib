package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.command.CommandSender;

public final class DoubleArg implements ArgumentType<Double> {

    @Override public String id() { return "<double>"; }

    @Override public Double parse(CommandSender s, TokenReader t) {
        String raw = t.next();
        if (raw == null) throw new ArgParseException("Expected " + id());
        try { return Double.parseDouble(raw); }
        catch (NumberFormatException e) { throw new ArgParseException("Expected <double>, got '" + raw + "'"); }
    }

}
