package com.muhammaddaffa.mdlib.commands.args;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@FunctionalInterface
public interface Suggester {

    List<String> get(CommandSender sender, String prefix);

    static Suggester list(List<String> values) {
        return (sender, prefix) -> {
            String p = prefix == null ? "" : prefix.toLowerCase();
            return values.stream()
                    .map(String::valueOf)
                    .filter(s -> s.toLowerCase().startsWith(p))
                    .toList();
        };
    }

    static Suggester supplier(Supplier<List<String>> supplier) {
        return (sender, prefix) -> {
            String p = prefix == null ? "" : prefix.toLowerCase();
            List<String> vals = supplier.get();
            return vals == null ? Collections.emptyList()
                    : vals.stream().filter(s -> s.toLowerCase().startsWith(p)).toList();
        };
    }

    static Suggester dynamic(BiFunction<CommandSender, String, List<String>> fn) {
        return fn::apply; // full control
    }

}
