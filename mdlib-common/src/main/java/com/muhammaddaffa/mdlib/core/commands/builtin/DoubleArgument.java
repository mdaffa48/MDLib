package com.muhammaddaffa.mdlib.core.commands.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.core.commands.Argument;

public class DoubleArgument<S> implements Argument<S, Double> {

    @Override public String id() { return "<double>"; }

    @Override public Double parse(S s, TokenReader t) {
        String raw = t.next();
        if (raw == null) throw new ArgParseException("Expected " + id());
        try { return Double.parseDouble(raw); }
        catch (NumberFormatException e) { throw new ArgParseException("Expected <double>, got '" + raw + "'"); }
    }

}
