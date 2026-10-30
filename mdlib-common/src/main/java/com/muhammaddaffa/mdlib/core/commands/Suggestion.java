package com.muhammaddaffa.mdlib.core.commands;

import com.muhammaddaffa.mdlib.commands.args.PartialContext;

import java.util.List;

@FunctionalInterface
public interface Suggestion<S> {
    List<String> suggest(S sender, String prefix, PartialContext prev);
}
