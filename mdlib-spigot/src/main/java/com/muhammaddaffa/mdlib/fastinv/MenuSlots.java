package com.muhammaddaffa.mdlib.fastinv;

import com.muhammaddaffa.mdlib.core.slots.SlotParser;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class MenuSlots {

    private MenuSlots() {
    }

    /**
     * Every slot set by an item's {@code slot} and {@code slots} keys, each slot once, in the order written.
     * Both keys accept {@code 13}, {@code "10-16"} or a list of both. Entries that are not a slot inside a menu of
     * {@code size} slots go to {@code invalidEntry} and are skipped. Pass the result to
     * {@link FastInv#setItems(Iterable, org.bukkit.inventory.ItemStack)}; the (slotFrom, slotTo) overload treats
     * slotTo as exclusive, unlike the inclusive ranges read here.
     */
    public static List<Integer> getItemSlots(@Nullable ConfigurationSection item, int size, Consumer<String> invalidEntry) {
        if (item == null) {
            return List.of();
        }
        return SlotParser.getItemSlots(item.get("slot"), item.get("slots"), size, invalidEntry);
    }

    /**
     * The slots in one config value: a number, a range like {@code "10-16"}, or a list of both.
     */
    public static List<Integer> parse(@Nullable Object value, int size, Consumer<String> invalidEntry) {
        return SlotParser.parse(value, size, invalidEntry);
    }

}
