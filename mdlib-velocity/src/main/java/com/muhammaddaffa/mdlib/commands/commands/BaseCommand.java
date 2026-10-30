package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.core.commands.BaseCommandSpec;
import com.velocitypowered.api.command.CommandSource;

public abstract class BaseCommand extends BaseCommandSpec<CommandSource> implements SimpleCommandSpec {

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
