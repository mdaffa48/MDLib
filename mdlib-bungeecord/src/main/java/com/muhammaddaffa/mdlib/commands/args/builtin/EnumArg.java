package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.core.commands.builtin.EnumArgument;
import net.md_5.bungee.api.CommandSender;

public final class EnumArg<E extends Enum<E>> extends EnumArgument<CommandSender, E> implements ArgumentType<E> {

    public EnumArg(Class<E> type) {
        super(type);
    }

}
