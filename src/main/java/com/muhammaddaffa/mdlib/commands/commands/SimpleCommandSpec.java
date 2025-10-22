package com.muhammaddaffa.mdlib.commands.commands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public interface SimpleCommandSpec {
    String name();
    default List<String> aliases() { return Collections.emptyList(); }
    default String description() { return ""; }
    default String usage() { return "/" + name(); }
    default String permission() { return null; }

    boolean execute(CommandSender sender, String label, String[] args);

    default List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return Collections.emptyList();
    }
}
