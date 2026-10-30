package com.muhammaddaffa.mdlib.core.commands.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.core.commands.Argument;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class EnumArgument<S, E extends Enum<E>> implements Argument<S, E> {

    private final Class<E> type;
    private final String id;

    public EnumArgument(Class<E> type) {
        this.type = type;
        this.id = "<" + type.getSimpleName().toLowerCase(Locale.ROOT) + ":" +
                String.join("|", Arrays.stream(type.getEnumConstants())
                        .map(e -> e.name().toLowerCase(Locale.ROOT)).toList())
                + ">";
    }

    @Override public String id() { return id; }

    @Override
    public E parse(S s, TokenReader t) {
        String raw = t.next();
        if (raw == null) throw new ArgParseException("Expected " + id());
        try { return Enum.valueOf(type, raw.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) {
            throw new ArgParseException("Expected " + id() + ", got '" + raw + "'");
        }
    }

    @Override
    public List<String> suggestions(S s, String prefix) {
        String pref = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return Arrays.stream(type.getEnumConstants())
                .map(e -> e.name().toLowerCase(Locale.ROOT))
                .filter(v -> v.startsWith(pref))
                .toList();
    }
}
