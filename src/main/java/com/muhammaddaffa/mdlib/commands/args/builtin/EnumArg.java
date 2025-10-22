package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class EnumArg<E extends Enum<E>> implements ArgumentType<E> {

    private final Class<E> type;
    private final String id;

    public EnumArg(Class<E> type) {
        this.type = type;
        this.id = "<" + type.getSimpleName().toLowerCase(Locale.ROOT) + ":" +
                String.join("|", Arrays.stream(type.getEnumConstants())
                        .map(e -> e.name().toLowerCase(Locale.ROOT)).toList())
                + ">";
    }

    @Override public String id() { return id; }

    @Override
    public E parse(CommandSender s, TokenReader t) {
        String raw = t.next();
        if (raw == null) throw new ArgParseException("Expected " + id());
        try { return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            throw new ArgParseException("Expected " + id() + ", got '" + raw + "'");
        }
    }

    @Override
    public List<String> suggestions(CommandSender s, String prefix) {
        String pref = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return Arrays.stream(type.getEnumConstants())
                .map(e -> e.name().toLowerCase(Locale.ROOT))
                .filter(v -> v.startsWith(pref))
                .toList();
    }
}
