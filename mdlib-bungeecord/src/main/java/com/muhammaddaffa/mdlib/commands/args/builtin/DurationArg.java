package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.core.commands.builtin.DurationArgument;
import net.md_5.bungee.api.CommandSender;

import java.util.List;

public final class DurationArg extends DurationArgument<CommandSender> implements ArgumentType<Integer> {

    /** Default suggestions: 10s, 30s, 1m, 5m, 1h */
    public DurationArg() {
        super();
    }

    public DurationArg(List<String> presets) {
        super(presets);
    }

}
