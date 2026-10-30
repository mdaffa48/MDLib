package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class LiteralArg implements ArgumentType<String> {

    private final List<String> literals;

    public LiteralArg(String... literals) {
        this.literals = Arrays.stream(literals).toList();
    }

    @Override public String id() { return "<" + String.join("|", literals) + ">"; }

    @Override
    public String parse(CommandSender sender, TokenReader tokens) throws ArgParseException {
        String next = tokens.next();
        if (next == null) throw new ArgParseException("Expected " + id());
        for (String lit : literals) {
            if (lit.equalsIgnoreCase(next)) return lit.toLowerCase(Locale.ROOT);
        }
        throw new ArgParseException("Expected one of: " + String.join(", ", literals));
    }

    @Override
    public List<String> suggestions(CommandSender sender, String prefix) {
        List<String> out = new ArrayList<>(literals.size());
        StringUtil.copyPartialMatches(prefix == null ? "" : prefix, literals, out);
        return out;
    }

    @Override public boolean greedy() { return false; }

}
