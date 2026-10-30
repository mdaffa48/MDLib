package com.muhammaddaffa.mdlib.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaperItemConfigTest {

    @Test
    void paperBuilderUsesTheSharedPlayerAwareParser() {
        Player player = mock(Player.class);
        ItemBuilder builder = mock(ItemBuilder.class);
        PluginManager plugins = mock(PluginManager.class);
        when(plugins.isPluginEnabled("PlaceholderAPI")).thenReturn(true);
        YamlConfiguration config = new YamlConfiguration();
        config.set("item.material", "{material}");
        config.set("item.amount", "%amount%");
        config.set("item.custom-model-data", "%model%");
        config.set("item.display-name", "%name%");
        config.set("item.lore", List.of("%name%"));
        config.set("item.flags", List.of("%flag%"));
        config.set("item.item-model", "%item_model%");
        Placeholder placeholder = new Placeholder().add("{material}", "%material%");

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
             MockedStatic<PlaceholderAPI> papi = mockStatic(PlaceholderAPI.class);
             MockedStatic<ItemBuilder> builders = mockStatic(ItemBuilder.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
            bukkit.when(Bukkit::getBukkitVersion).thenReturn("26.2-test");
            papi.when(() -> PlaceholderAPI.setPlaceholders(eq(player), anyString()))
                    .thenAnswer(invocation -> switch (invocation.<String>getArgument(1)) {
                        case "%material%" -> "STONE";
                        case "%amount%" -> "4";
                        case "%model%" -> "123";
                        case "%name%" -> "<green>Player";
                        case "%flag%" -> "hide_enchants";
                        case "%item_model%" -> "test:reward";
                        default -> invocation.getArgument(1);
                    });
            builders.when(() -> ItemBuilder.retrieveItemBuilder("STONE")).thenReturn(builder);

            assertSame(builder, PaperItemBuilder.fromConfig(player, config, "item", placeholder));
            verify(builder).amount(4);
            verify(builder).customModelData(123);
            verify(builder).name("<green>Player");
            verify(builder).lore(List.of("<green>Player"));
            verify(builder).flags(ItemFlag.HIDE_ENCHANTS);
            verify(builder).itemModel(NamespacedKey.fromString("test:reward"));
            assertEquals("{material}", config.getString("item.material"));
            assertNull(PaperItemBuilder.fromConfig(config, "missing"));
        }
    }
}
