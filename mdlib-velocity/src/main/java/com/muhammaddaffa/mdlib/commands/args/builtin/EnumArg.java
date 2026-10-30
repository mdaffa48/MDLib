package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.core.commands.builtin.EnumArgument;
import com.velocitypowered.api.command.CommandSource;

public final class EnumArg<E extends Enum<E>> extends EnumArgument<CommandSource, E> implements ArgumentType<E> {

    public EnumArg(Class<E> type) {
        super(type);
    }

}
