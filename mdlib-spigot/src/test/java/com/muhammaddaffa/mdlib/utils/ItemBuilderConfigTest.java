package com.muhammaddaffa.mdlib.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ItemBuilderConfigTest {

    @Test
    void resolvesEveryItemValueBeforeTypedReadsWithoutChangingTheSource() {
        ConfigurationSection section = new MemoryConfiguration();
        Map<String, String> values = Map.ofEntries(
                Map.entry("material", "nexo;player_sword"),
                Map.entry("custom-model-data", "123"),
                Map.entry("item-model", "test:player_sword"),
                Map.entry("amount", "4"),
                Map.entry("display-name", "<green>Player's sword"),
                Map.entry("color.r", "12"),
                Map.entry("color.g", "34"),
                Map.entry("color.b", "56"));
        values.forEach((key, value) -> section.set(key, "%" + key + "%"));
        section.set("lore", List.of("%display-name%", "%amount%"));
        section.set("flags", List.of("%flag%"));
        section.set("enchantments", List.of("%enchantment%;%level%"));
        Placeholder placeholder = new Placeholder()
                .add("%flag%", "HIDE_ENCHANTS")
                .add("%enchantment%", "SHARPNESS")
                .add("%level%", 5);
        values.forEach((key, value) -> placeholder.add("%" + key + "%", value));

        ConfigurationSection resolved = ItemBuilder.resolveConfig(section, placeholder::translate);

        values.forEach((key, value) -> assertEquals(value, resolved.getString(key), key));
        assertEquals(4, ItemBuilder.configInt(resolved, "amount", 0));
        assertEquals(123, ItemBuilder.configInt(resolved, "custom-model-data", 0));
        assertEquals(12, ItemBuilder.configInt(resolved, "color.r", 255));
        assertEquals(List.of("<green>Player's sword", "4"), resolved.getStringList("lore"));
        assertEquals(List.of("HIDE_ENCHANTS"), resolved.getStringList("flags"));
        assertEquals(List.of("SHARPNESS;5"), resolved.getStringList("enchantments"));
        assertEquals("%material%", section.getString("material"));
        assertEquals(List.of("%flag%"), section.getStringList("flags"));
    }

    @Test
    void keepsDefaultsAndResolvesInheritedValues() {
        MemoryConfiguration defaults = new MemoryConfiguration();
        defaults.set("item.material", "%material%");
        defaults.set("item.amount", "%amount%");
        defaults.set("item.color.r", "%red%");
        MemoryConfiguration config = new MemoryConfiguration(defaults);
        config.set("item.amount", 2);
        Placeholder placeholder = new Placeholder().add("%material%", "STONE").add("%red%", 20);

        ConfigurationSection resolved = ItemBuilder.resolveConfig(
                config.getConfigurationSection("item"), placeholder::translate);

        assertEquals("STONE", resolved.getString("material"));
        assertEquals(2, ItemBuilder.configInt(resolved, "amount", 0));
        assertEquals(20, ItemBuilder.configInt(resolved, "color.r", 255));
    }

    @Test
    void preservesTypedNumbersAndUsesDefaultsForUnresolvedOrInvalidNumbers() {
        ConfigurationSection section = new MemoryConfiguration();
        section.set("number", 3);
        section.set("decimal", 3.5);
        section.set("text", " 7 ");
        section.set("unresolved", "%missing_expansion%");
        section.set("invalid", "invalid");

        ConfigurationSection resolved = ItemBuilder.resolveConfig(section, value -> value);

        assertEquals(3, ItemBuilder.configInt(resolved, "number", 0));
        assertEquals(3, ItemBuilder.configInt(resolved, "decimal", 0));
        assertEquals(7, ItemBuilder.configInt(resolved, "text", 0));
        assertEquals(255, ItemBuilder.configInt(resolved, "unresolved", 255));
        assertEquals(0, ItemBuilder.configInt(resolved, "invalid", 0));
        assertEquals(255, ItemBuilder.configInt(resolved, "missing", 255));
    }

    @Test
    void missingSectionsRemainNullWithBothOldAndPlayerAwareOverloads() {
        YamlConfiguration config = new YamlConfiguration();

        assertNull(ItemBuilder.fromConfig((ConfigurationSection) null));
        assertNull(ItemBuilder.fromConfig((ConfigurationSection) null, null));
        assertNull(ItemBuilder.fromConfig(null, (ConfigurationSection) null));
        assertNull(ItemBuilder.fromConfig(null, (ConfigurationSection) null, null));
        assertNull(ItemBuilder.fromConfig(null, config, "missing"));
        assertNull(ItemBuilder.fromConfig(null, config, "missing", null));
    }
}
