package com.muhammaddaffa.mdlib.core.commands.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.core.commands.Argument;

public class GreedyStringArgument<S> implements Argument<S, String> {

    @Override public String id() { return "<text...>"; }
    @Override public boolean greedy() { return true; }

    @Override
    public String parse(S s, TokenReader t) {
        String rest = t.remainingJoined();
        if (rest.isEmpty()) throw new ArgParseException("Expected " + id());
        return rest;
    }

}
