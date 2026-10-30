package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.core.commands.CommandSpec;
import net.md_5.bungee.api.CommandSender;

public interface SimpleCommandSpec extends CommandSpec<CommandSender> {

    default void register() {
        MDLib.getCommandRegistry().register(this);
    }

}
