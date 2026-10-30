package com.muhammaddaffa.mdlib.commands.args;

import com.muhammaddaffa.mdlib.core.commands.Completions;
import com.muhammaddaffa.mdlib.core.commands.Suggestion;
import com.velocitypowered.api.command.CommandSource;

import java.util.List;
import java.util.function.BiFunction;

@FunctionalInterface
public interface ArgSuggester extends Suggestion<CommandSource> {

    static ArgSuggester ofList(List<String> values) {
        return (sender, prefix, prev) -> Completions.partialMatches(prefix == null ? "" : prefix, values);
    }

    static ArgSuggester ofDynamic(BiFunction<CommandSource, String, List<String>> fn) {
        return (sender, prefix, prev) -> fn.apply(sender, prefix);
    }

    static ArgSuggester ofDynamic(TriFunction<CommandSource, String, PartialContext, List<String>> fn) {
        return fn::apply;
    }


    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

}
