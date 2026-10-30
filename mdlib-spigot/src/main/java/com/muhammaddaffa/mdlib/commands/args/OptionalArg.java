package com.muhammaddaffa.mdlib.commands.args;

import org.bukkit.command.CommandSender;

import java.util.List;

public final class OptionalArg<T> implements ArgumentType<T> {
    private final ArgumentType<T> inner;

    private OptionalArg(ArgumentType<T> inner) { this.inner = inner; }
    public static <T> OptionalArg<T> of(ArgumentType<T> inner) { return new OptionalArg<>(inner); }

    @Override public String id() { return "[" + inner.id().replaceAll("[\\[\\]<>]", "") + "]"; }
    @Override public boolean greedy() { return inner.greedy(); }

    @Override
    public T parse(CommandSender sender, TokenReader tokens) {
        if (!tokens.hasNext()) return null;       // optional: not present
        return inner.parse(sender, tokens);
    }

    @Override
    public List<String> suggestions(CommandSender sender, String prefix) {
        return inner.suggestions(sender, prefix);
    }
}
