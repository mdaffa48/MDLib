package com.muhammaddaffa.mdlib.core.commands.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.core.commands.Argument;

public class StringArgument<S> implements Argument<S, String> {

    @Override
    public String id() {
        return "<text>";
    }

    @Override
    public String parse(S sender, TokenReader tokens) throws ArgParseException {
        String next = tokens.next();
        if (next == null || next.isEmpty()) {
            throw new ArgParseException("Expected " + id());
        }
        return next;
    }

}
