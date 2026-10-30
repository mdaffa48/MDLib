package com.muhammaddaffa.mdlib.core.commands.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.core.commands.Argument;
import com.muhammaddaffa.mdlib.core.commands.Completions;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class LiteralArgument<S> implements Argument<S, String> {

    private final List<String> literals;

    public LiteralArgument(String... literals) {
        this.literals = Arrays.stream(literals).toList();
    }

    @Override public String id() { return "<" + String.join("|", literals) + ">"; }

    @Override
    public String parse(S sender, TokenReader tokens) throws ArgParseException {
        String next = tokens.next();
        if (next == null) throw new ArgParseException("Expected " + id());
        for (String lit : literals) {
            if (lit.equalsIgnoreCase(next)) return lit.toLowerCase(Locale.ROOT);
        }
        throw new ArgParseException("Expected one of: " + String.join(", ", literals));
    }

    @Override
    public List<String> suggestions(S sender, String prefix) {
        return Completions.partialMatches(prefix == null ? "" : prefix, literals);
    }

    @Override public boolean greedy() { return false; }

}
