package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class DurationArg implements ArgumentType<Integer> {

    private static final Map<Character, Integer> UNIT_TO_SECONDS = Map.of(
            's', 1,
            'm', 60,
            'h', 3600,
            'd', 86400
    );

    private final List<String> presets;

    /** Default suggestions: 10s, 30s, 1m, 5m, 1h */
    public DurationArg() {
        this(List.of("10s", "30s", "1m", "5m", "1h"));
    }

    public DurationArg(List<String> presets) {
        this.presets = presets == null ? List.of() : presets;
    }

    @Override
    public String id() {
        return "<duration>";
    }

    @Override
    public Integer parse(CommandSender sender, TokenReader tokens) throws ArgParseException {
        String raw = tokens.next();
        if (raw == null || raw.isEmpty()) {
            throw new ArgParseException("Expected " + id());
        }

        try {
            // If the input is just digits, assume seconds
            if (raw.chars().allMatch(Character::isDigit)) {
                return Integer.parseInt(raw);
            }

            // Parse last character as unit
            char unit = Character.toLowerCase(raw.charAt(raw.length() - 1));
            Integer multiplier = UNIT_TO_SECONDS.get(unit);
            if (multiplier == null) {
                throw new NumberFormatException();
            }

            int number = Integer.parseInt(raw.substring(0, raw.length() - 1));
            if (number < 0) {
                throw new NumberFormatException();
            }

            long seconds = (long) number * multiplier;
            if (seconds > Integer.MAX_VALUE) {
                throw new ArgParseException("Duration too large (max ~68 years)");
            }

            return (int) seconds;

        } catch (NumberFormatException e) {
            throw new ArgParseException("Use number with unit: e.g., 10s, 5m, 2h, 1d");
        }
    }

    @Override
    public List<String> suggestions(CommandSender sender, String prefix) {
        ArrayList<String> out = new ArrayList<>();
        StringUtil.copyPartialMatches(prefix == null ? "" : prefix.toLowerCase(Locale.ROOT), presets, out);
        return out;
    }
}
