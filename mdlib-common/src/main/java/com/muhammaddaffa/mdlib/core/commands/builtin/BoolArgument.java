package com.muhammaddaffa.mdlib.core.commands.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.core.commands.Argument;

public class BoolArgument<S> implements Argument<S, Boolean> {

    @Override public String id() { return "<bool>"; }

    @Override public Boolean parse(S s, TokenReader t) {
        String raw = t.next();
        if (raw == null) throw new ArgParseException("Expected " + id());
        switch (raw.toLowerCase()) {
            case "true","yes","y","on","1" -> { return true; }
            case "false","no","n","off","0" -> { return false; }
            default -> throw new ArgParseException("Expected <bool>, got '" + raw + "'");
        }
    }

}
