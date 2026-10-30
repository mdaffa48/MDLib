package com.muhammaddaffa.mdlib.commands.args;

import com.muhammaddaffa.mdlib.core.commands.OptionalArgument;
import net.md_5.bungee.api.CommandSender;

public final class OptionalArg<T> extends OptionalArgument<CommandSender, T> implements ArgumentType<T> {

    private OptionalArg(ArgumentType<T> inner) { super(inner); }

    public static <T> OptionalArg<T> of(ArgumentType<T> inner) { return new OptionalArg<>(inner); }

}
