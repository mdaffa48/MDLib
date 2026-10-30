package com.muhammaddaffa.mdlib.core.commands;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;

import java.util.Collections;
import java.util.List;

public interface Argument<S, T> {
    String id();

    T parse(S sender, TokenReader tokens) throws ArgParseException;

    default List<String> suggestions(S sender, String prefix) { return Collections.emptyList(); }

    default boolean greedy() { return false; }
}
