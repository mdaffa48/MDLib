package com.muhammaddaffa.mdlib.core.commands.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.core.commands.Argument;

public class IntArgument<S> implements Argument<S, Integer> {

    @Override public String id() { return "<number>"; }

    @Override public Integer parse(S s, TokenReader t) {
        String raw = t.next();
        if (raw == null) throw new ArgParseException("Expected " + id());
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { throw new ArgParseException("Expected " + id() + ", got '" + raw + "'"); }
    }

}
