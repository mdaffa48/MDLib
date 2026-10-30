package com.muhammaddaffa.mdlib.fastinv;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuSlotsTest {

    private final List<String> invalid = new ArrayList<>();

    private ConfigurationSection item(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return config.getConfigurationSection("item");
    }

    @Test
    void readsSlotAndSlotsKeysTheWayDeluxeMenusWritesThem() throws InvalidConfigurationException {
        assertEquals(List.of(13), MenuSlots.getItemSlots(item("item:\n  slot: 13\n"), 27, invalid::add));
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16, 22),
                MenuSlots.getItemSlots(item("item:\n  slots: [10-16, 22]\n"), 27, invalid::add));
        assertEquals(List.of(4, 0, 1, 2, 3),
                MenuSlots.getItemSlots(item("item:\n  slot: 4\n  slots:\n    - 0-4\n"), 9, invalid::add));
        assertEquals(List.of(0, 1, 2), MenuSlots.getItemSlots(item("item:\n  slots: [0, 1, 2]\n"), 9, invalid::add));
        assertTrue(invalid.isEmpty());
    }

    @Test
    void missingSectionsAndKeysGiveNoSlots() throws InvalidConfigurationException {
        assertEquals(List.of(), MenuSlots.getItemSlots(null, 27, invalid::add));
        assertEquals(List.of(), MenuSlots.getItemSlots(item("item:\n  material: STONE\n"), 27, invalid::add));
        assertTrue(invalid.isEmpty());
    }

    @Test
    void typosAreReportedAndSkipped() throws InvalidConfigurationException {
        assertEquals(List.of(8), MenuSlots.getItemSlots(item("item:\n  slot: 30\n  slots: [8, '7-a', 3-1]\n"), 9, invalid::add));
        assertEquals(List.of("30", "7-a", "3-1"), invalid);
    }

    @Test
    void parseMatchesTheSharedParser() {
        assertEquals(List.of(1, 2, 2), MenuSlots.parse(List.of("1-2", 2), 9, invalid::add));
    }
}
