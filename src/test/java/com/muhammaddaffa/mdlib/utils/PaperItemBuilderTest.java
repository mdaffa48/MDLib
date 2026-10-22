package com.muhammaddaffa.mdlib.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaperItemBuilderTest {

    @Test
    void pendingTextSurvivesTypeAndPlaceholder() {
        ItemMeta result = new PaperItemBuilder(item())
                .name(Component.text("%player%'s sword"))
                .meta(meta -> meta.lore(List.of(Component.text("For %player%"))))
                .type(Material.DIAMOND_SWORD)
                .placeholder(new PlaceholderComponent().add("%player%", "Alex"))
                .build().getItemMeta();

        assertEquals(Component.text("Alex's sword"), result.displayName().compact());
        assertEquals(List.of(Component.text("For Alex")), result.lore().stream().map(Component::compact).toList());
    }

    @Test
    void pendingNameAndLoreSurviveTypeAndTextEdits() {
        ItemStack item = item();
        ItemMeta result = new PaperItemBuilder(item)
                .name(Component.text("%player%'s sword"))
                .lore(Component.text("First %player%"), Component.text("{details}"))
                .type(Material.DIAMOND_SWORD)
                .addLore(Component.text("Last %player%"))
                .addLore(List.of(Component.text("Extra")))
                .loreCustom("{details}", Component.text("Details %player%"),
                        new PlaceholderComponent().add("%player%", "Alex"))
                .placeholder(new PlaceholderComponent().add("%player%", "Alex"))
                .build().getItemMeta();

        assertEquals(Component.text("Alex's sword"), result.displayName().compact());
        assertEquals(List.of(Component.text("First Alex"), Component.text("Details Alex"),
                Component.text("Last Alex"), Component.text("Extra")),
                result.lore().stream().map(Component::compact).toList());
    }

    @Test
    void loreCustomMatchesMarkersAcrossStyledChildrenAndKeepsOtherComponents() {
        Component marker = Component.text("before {de")
                .append(Component.text("tails}", NamedTextColor.GOLD))
                .append(Component.text(" after"));
        Component unchanged = Component.text("Keep this", NamedTextColor.GREEN);
        Component replacement = Component.text("Details", NamedTextColor.BLUE);

        ItemMeta result = new PaperItemBuilder(item())
                .lore(unchanged, marker)
                .loreCustom("{details}", replacement)
                .build().getItemMeta();

        assertEquals(List.of(unchanged, replacement), result.lore());
    }

    @Test
    void flagsAndEveryPersistentDataTypeSurviveBuildAndLaterEdits() {
        NamespacedKey key = new NamespacedKey("test", "value");
        PaperItemBuilder builder = new PaperItemBuilder(item());
        builder.flags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
                .removeFlags(ItemFlag.HIDE_ATTRIBUTES)
                .pdc(key, "text");

        ItemMeta first = builder.build().getItemMeta();
        assertEquals(EnumSet.of(ItemFlag.HIDE_ENCHANTS), first.getItemFlags());
        assertEquals("text", first.getPersistentDataContainer().get(key, PersistentDataType.STRING));
        assertEquals(2.5, builder.pdc(key, 2.5).build().getItemMeta()
                .getPersistentDataContainer().get(key, PersistentDataType.DOUBLE));
        assertEquals(2.5F, builder.pdc(key, 2.5F).build().getItemMeta()
                .getPersistentDataContainer().get(key, PersistentDataType.FLOAT));
        assertEquals(2, builder.pdc(key, 2).build().getItemMeta()
                .getPersistentDataContainer().get(key, PersistentDataType.INTEGER));
        assertEquals(2L, builder.pdc(key, 2L).build().getItemMeta()
                .getPersistentDataContainer().get(key, PersistentDataType.LONG));
        assertEquals((byte) 2, builder.pdc(key, (byte) 2).removeFlags().build().getItemMeta()
                .getPersistentDataContainer().get(key, PersistentDataType.BYTE));
        assertTrue(builder.build().getItemMeta().getItemFlags().isEmpty());
    }

    @Test
    void legacyDurabilitySurvivesTheFinalMetadataWrite() {
        ItemMeta result = new ItemBuilder(item()).durability((short) 42).build().getItemMeta();

        assertEquals(42, ((Damageable) result).getDamage());
    }

    private static ItemStack item() {
        ItemStack item = mock(ItemStack.class);
        AtomicReference<ItemMeta> live = new AtomicReference<>(new Metadata().meta);
        when(item.getItemMeta()).thenAnswer(invocation -> live.get().clone());
        doAnswer(invocation -> {
            live.set(invocation.<ItemMeta>getArgument(0).clone());
            return true;
        }).when(item).setItemMeta(any(ItemMeta.class));
        when(item.withType(any(Material.class))).thenAnswer(invocation -> item);
        when(item.displayName()).thenAnswer(invocation -> live.get().displayName());
        when(item.lore()).thenAnswer(invocation -> live.get().lore());
        doAnswer(invocation -> {
            live.get().addItemFlags((ItemFlag[]) invocation.getRawArguments()[0]);
            return null;
        }).when(item).addItemFlags(any(ItemFlag[].class));
        doAnswer(invocation -> {
            live.get().removeItemFlags((ItemFlag[]) invocation.getRawArguments()[0]);
            return null;
        }).when(item).removeItemFlags(any(ItemFlag[].class));
        when(item.editPersistentDataContainer(any())).thenAnswer(invocation -> {
            invocation.<Consumer<PersistentDataContainer>>getArgument(0)
                    .accept(live.get().getPersistentDataContainer());
            return true;
        });
        doAnswer(invocation -> {
            ((Damageable) live.get()).setDamage(invocation.<Short>getArgument(0));
            return null;
        }).when(item).setDurability(anyShort());
        return item;
    }

    private static final class Metadata {
        private Component name;
        private List<Component> lore;
        private int damage;
        private final EnumSet<ItemFlag> flags = EnumSet.noneOf(ItemFlag.class);
        private final Map<NamespacedKey, Object> data = new HashMap<>();
        private final Damageable meta = mock(Damageable.class);

        private Metadata() {
            when(meta.clone()).thenAnswer(invocation -> {
                Metadata copy = new Metadata();
                copy.name = name;
                copy.lore = lore == null ? null : new ArrayList<>(lore);
                copy.damage = damage;
                copy.flags.addAll(flags);
                copy.data.putAll(data);
                return copy.meta;
            });
            when(meta.displayName()).thenAnswer(invocation -> name);
            doAnswer(invocation -> {
                name = invocation.getArgument(0);
                return null;
            }).when(meta).displayName(any());
            when(meta.lore()).thenAnswer(invocation -> lore == null ? null : List.copyOf(lore));
            doAnswer(invocation -> {
                lore = new ArrayList<>(invocation.<List<Component>>getArgument(0));
                return null;
            }).when(meta).lore(anyList());
            when(meta.getItemFlags()).thenAnswer(invocation -> EnumSet.copyOf(flags));
            doAnswer(invocation -> flags.addAll(Arrays.asList((ItemFlag[]) invocation.getRawArguments()[0])))
                    .when(meta).addItemFlags(any(ItemFlag[].class));
            doAnswer(invocation -> flags.removeAll(Arrays.asList((ItemFlag[]) invocation.getRawArguments()[0])))
                    .when(meta).removeItemFlags(any(ItemFlag[].class));
            when(meta.getDamage()).thenAnswer(invocation -> damage);
            doAnswer(invocation -> {
                damage = invocation.getArgument(0);
                return null;
            }).when(meta).setDamage(anyInt());
            PersistentDataContainer pdc = mock(PersistentDataContainer.class);
            when(meta.getPersistentDataContainer()).thenReturn(pdc);
            doAnswer(invocation -> data.put(invocation.getArgument(0), invocation.getArgument(2)))
                    .when(pdc).set(any(), any(), any());
            when(pdc.get(any(), any())).thenAnswer(invocation -> data.get(invocation.getArgument(0)));
        }
    }
}
