package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.core.commands.builtin.LiteralArgument;
import net.md_5.bungee.api.CommandSender;

public final class LiteralArg extends LiteralArgument<CommandSender> implements ArgumentType<String> {

    public LiteralArg(String... literals) {
        super(literals);
    }

}
