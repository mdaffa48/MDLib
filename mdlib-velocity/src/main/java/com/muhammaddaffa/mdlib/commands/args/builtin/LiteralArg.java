package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.core.commands.builtin.LiteralArgument;
import com.velocitypowered.api.command.CommandSource;

public final class LiteralArg extends LiteralArgument<CommandSource> implements ArgumentType<String> {

    public LiteralArg(String... literals) {
        super(literals);
    }

}
