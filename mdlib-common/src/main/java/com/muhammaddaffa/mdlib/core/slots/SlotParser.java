package com.muhammaddaffa.mdlib.core.slots;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SlotParser {

    // A slot or an inclusive range of slots, "13" or "10-16". Nine digits at most so parseInt can't overflow.
    private static final Pattern SLOT = Pattern.compile("(\\d{1,9})(?:\\s*-\\s*(\\d{1,9}))?");

    private SlotParser() {
    }

    /**
     * Every slot set by an item's {@code slot} and {@code slots} values, each slot once, in the order written
     * ({@code slot} first). Entries that are not a slot inside a menu of {@code size} slots go to
     * {@code invalidEntry} and are skipped.
     */
    public static List<Integer> getItemSlots(@Nullable Object slot, @Nullable Object slots, int size, Consumer<String> invalidEntry) {
        Set<Integer> result = new LinkedHashSet<>();
        result.addAll(parse(slot, size, invalidEntry));
        result.addAll(parse(slots, size, invalidEntry));
        return new ArrayList<>(result);
    }

    /**
     * The slots in one config value: a number, a range like {@code "10-16"}, or a list of both. Duplicates are kept.
     * An entry that is not a number or range, a range written backwards, or a slot at or past {@code size}
     * goes to {@code invalidEntry} (trimmed) and is skipped.
     */
    public static List<Integer> parse(@Nullable Object value, int size, Consumer<String> invalidEntry) {
        List<?> entries = value instanceof List<?> list ? list : value == null ? List.of() : List.of(value);
        List<Integer> slots = new ArrayList<>();
        for (Object entry : entries) {
            String text = String.valueOf(entry).trim();
            Matcher matcher = SLOT.matcher(text);
            if (!matcher.matches()) {
                invalidEntry.accept(text);
                continue;
            }
            int from = Integer.parseInt(matcher.group(1));
            int to = matcher.group(2) == null ? from : Integer.parseInt(matcher.group(2));
            // A slot past the end of the menu would make Inventory#setItem throw on every refresh.
            if (from > to || to >= size) {
                invalidEntry.accept(text);
                continue;
            }
            for (int slot = from; slot <= to; slot++) {
                slots.add(slot);
            }
        }
        return slots;
    }

}
