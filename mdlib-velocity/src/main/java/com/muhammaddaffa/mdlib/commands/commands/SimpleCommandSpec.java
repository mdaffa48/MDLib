package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.core.commands.CommandSpec;
import com.velocitypowered.api.command.CommandSource;

public interface SimpleCommandSpec extends CommandSpec<CommandSource> {

    default void register() {
        MDLib.getCommandRegistry().register(this);
    }

}
