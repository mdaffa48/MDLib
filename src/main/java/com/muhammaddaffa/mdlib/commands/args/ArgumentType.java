package com.muhammaddaffa.mdlib.commands.args;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface ArgumentType<T> {
    String id();

    T parse(CommandSender sender, TokenReader tokens) throws ArgParseException;

    default List<String> suggestions(CommandSender sender, String prefix) { return Collections.emptyList(); }

    default boolean greedy() { return false; }
}
