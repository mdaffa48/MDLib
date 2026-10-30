package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.core.commands.BaseCommandSpec;
import net.md_5.bungee.api.CommandSender;

public abstract class BaseCommand extends BaseCommandSpec<CommandSender> implements SimpleCommandSpec {

    public BaseCommand(String name) {
        super(name);
    }

    public BaseCommand(String name, String permission) {
        super(name, permission);
    }

    public BaseCommand(String name, String description, String usage, String permission) {
        super(name, description, usage, permission);
    }

}
