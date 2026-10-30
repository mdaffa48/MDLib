package com.muhammaddaffa.mdlib.core.commands;

import com.muhammaddaffa.mdlib.commands.args.TokenReader;

import java.util.List;

public class OptionalArgument<S, T> implements Argument<S, T> {
    private final Argument<S, T> inner;

    protected OptionalArgument(Argument<S, T> inner) { this.inner = inner; }

    /**
     * Wraps {@code inner} so a missing token parses to null instead of failing.
     */
    public static <S, T> OptionalArgument<S, T> optional(Argument<S, T> inner) { return new OptionalArgument<>(inner); }

    @Override public String id() { return "[" + inner.id().replaceAll("[\\[\\]<>]", "") + "]"; }
    @Override public boolean greedy() { return inner.greedy(); }

    @Override
    public T parse(S sender, TokenReader tokens) {
        if (!tokens.hasNext()) return null;       // optional: not present
        return inner.parse(sender, tokens);
    }

    @Override
    public List<String> suggestions(S sender, String prefix) {
        return inner.suggestions(sender, prefix);
    }
}
