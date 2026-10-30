package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.command.CommandSender;

public final class BoolArg implements ArgumentType<Boolean> {

    @Override public String id() { return "<bool>"; }

    @Override public Boolean parse(CommandSender s, TokenReader t) {
        String raw = t.next();
        if (raw == null) throw new ArgParseException("Expected " + id());
        switch (raw.toLowerCase()) {
            case "true","yes","y","on","1" -> { return true; }
            case "false","no","n","off","0" -> { return false; }
            default -> throw new ArgParseException("Expected <bool>, got '" + raw + "'");
        }
    }

}