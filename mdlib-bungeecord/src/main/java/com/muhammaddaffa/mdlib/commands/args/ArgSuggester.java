package com.muhammaddaffa.mdlib.commands.args;

import com.muhammaddaffa.mdlib.core.commands.Completions;
import com.muhammaddaffa.mdlib.core.commands.Suggestion;
import net.md_5.bungee.api.CommandSender;

import java.util.List;
import java.util.function.BiFunction;

@FunctionalInterface
public interface ArgSuggester extends Suggestion<CommandSender> {

    static ArgSuggester ofList(List<String> values) {
        return (sender, prefix, prev) -> Completions.partialMatches(prefix == null ? "" : prefix, values);
    }

    static ArgSuggester ofDynamic(BiFunction<CommandSender, String, List<String>> fn) {
        return (sender, prefix, prev) -> fn.apply(sender, prefix);
    }

    static ArgSuggester ofDynamic(TriFunction<CommandSender, String, PartialContext, List<String>> fn) {
        return fn::apply;
    }


    @FunctionalInterface
    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }

}
