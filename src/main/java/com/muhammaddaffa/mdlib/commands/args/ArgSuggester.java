package com.muhammaddaffa.mdlib.commands.args;

import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

@FunctionalInterface
public interface ArgSuggester {
    List<String> suggest(CommandSender sender, String prefix, PartialContext prev);

    // Helpers:

    static ArgSuggester ofList(List<String> values) {
        return (sender, prefix, prev) -> {
            List<String> out = new ArrayList<>();
            StringUtil.copyPartialMatches(prefix == null ? "" : prefix, values, out);
            return out;
        };
    }

    static ArgSuggester ofDynamic(BiFunction<CommandSender, String, List<String>> fn) {
        return (sender, prefix, prev) -> fn.apply(sender, prefix);
    }
}
