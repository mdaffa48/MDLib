package com.muhammaddaffa.mdlib.core.commands;

import java.util.Collections;
import java.util.List;

public interface CommandSpec<S> {
    String name();
    default List<String> aliases() { return Collections.emptyList(); }
    default String description() { return ""; }
    default String usage() { return "/" + name(); }
    default String permission() { return null; }

    void execute(S sender, String label, String[] args);

    default List<String> tabComplete(S sender, String alias, String[] args) {
        return Collections.emptyList();
    }
}
