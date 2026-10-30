package com.muhammaddaffa.mdlib.utils;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Simple {@link ItemStack} builder
 *
 * @author MrMicky
 */
public class PaperItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    public PaperItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    public PaperItemBuilder(@NotNull ItemStack item) {
        this.item = Objects.requireNonNull(item, "item");
        this.meta = item.getItemMeta();

        if (this.meta == null) {
            throw new IllegalArgumentException("The type " + item.getType() + " doesn't support item meta");
        }
    }

    public ItemMeta getItemMeta() {
        return meta;
    }

    public PaperItemBuilder type(@NotNull Material material) {
        this.item.setItemMeta(this.meta);
        this.item = this.item.withType(material);
        this.meta = this.item.getItemMeta();
        return this;
    }

    public PaperItemBuilder durability(int durability) {
        this.item.setItemMeta(this.meta);
        this.item.setData(DataComponentTypes.DAMAGE, durability);
        this.meta = this.item.getItemMeta();
        return this;
    }

    public PaperItemBuilder amount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    public PaperItemBuilder enchant(Enchantment enchantment) {
        return enchant(enchantment, 1);
    }

    public PaperItemBuilder enchant(Enchantment enchantment, int level) {
        this.item.setItemMeta(this.meta);
        this.item.addEnchantment(enchantment, level);
        this.meta = this.item.getItemMeta();
        return this;
    }

    public PaperItemBuilder removeEnchant(Enchantment enchantment) {
        this.meta.removeEnchant(enchantment);
        return this;
    }

    public PaperItemBuilder removeEnchants() {
        this.meta.getEnchants().keySet().forEach(this.meta::removeEnchant);
        return this;
    }

    public PaperItemBuilder meta(Consumer<ItemMeta> metaConsumer) {
        metaConsumer.accept(this.meta);
        return this;
    }

    public <T extends ItemMeta> PaperItemBuilder meta(Class<T> metaClass, Consumer<T> metaConsumer) {
        if (metaClass.isInstance(this.meta)) {
            metaConsumer.accept(metaClass.cast(this.meta));
        }
        return this;
    }

    public PaperItemBuilder name(String name) {
        this.meta.displayName(ColorComponent.colorToComponent(name));
        return this;
    }

    public PaperItemBuilder name(Component name) {
        this.meta.displayName(name);
        return this;
    }

    public PaperItemBuilder lore(Component lore) {
        return lore(Collections.singletonList(lore));
    }

    public PaperItemBuilder lore(Component... lore) {
        return lore(Arrays.asList(lore));
    }

    public PaperItemBuilder lore(List<Component> lore) {
        this.meta.lore(lore);
        return this;
    }

    public PaperItemBuilder addLore(Component line) {
        return addLore(Collections.singletonList(line));
    }

    public PaperItemBuilder addLore(Component... lines) {
        return addLore(Arrays.asList(lines));
    }

    public PaperItemBuilder addLore(List<Component> lines) {
        List<Component> lore = this.meta.lore();

        if (lore == null) {
            return lore(lines);
        }

        lore = new ArrayList<>(lore);
        lore.addAll(lines);
        return lore(lore);
    }

    public PaperItemBuilder flags(ItemFlag... flags) {
        this.meta.addItemFlags(flags);
        return this;
    }

    public PaperItemBuilder flags() {
        return flags(ItemFlag.values());
    }

    public PaperItemBuilder removeFlags(ItemFlag... flags) {
        this.meta.removeItemFlags(flags);
        return this;
    }

    public PaperItemBuilder removeFlags() {
        return removeFlags(ItemFlag.values());
    }

    public PaperItemBuilder armorColor(Color color) {
        return meta(LeatherArmorMeta.class, m -> m.setColor(color));
    }

    @Deprecated(since = "1.21.5")
    public PaperItemBuilder customModelData(int data) {
        this.meta.setCustomModelData(data);
        return this;
    }

    public PaperItemBuilder itemModel(NamespacedKey key) {
        this.meta.setItemModel(key);
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, String s) {
        this.meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, s);
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Double d) {
        this.meta.getPersistentDataContainer().set(key, PersistentDataType.DOUBLE, d);
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Float f) {
        this.meta.getPersistentDataContainer().set(key, PersistentDataType.FLOAT, f);
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Integer i) {
        this.meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, i);
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Long l) {
        this.meta.getPersistentDataContainer().set(key, PersistentDataType.LONG, l);
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Byte b) {
        this.meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, b);
        return this;
    }

    /**
     * Applies a player name, UUID, texture hash, Minecraft texture URL, or Base64 texture.
     * Unrecognized identifiers and items without skull metadata are left unchanged.
     */
    public PaperItemBuilder skull(String identifier) {
        Objects.requireNonNull(identifier, "identifier");
        if (!(this.meta instanceof SkullMeta skullMeta)) {
            return this;
        }

        String value = identifier.trim();
        if (value.matches("[A-Za-z0-9_]{1,16}")) {
            skullMeta.setPlayerProfile(Bukkit.createProfile(value));
            return this;
        }
        if (value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            return skull(UUID.fromString(value));
        }

        String texture = value;
        if (value.matches("(?i)(?:(?:https?://)?(?:textures\\.)?minecraft\\.net/texture/)?[0-9a-f]{55,70}")) {
            String hash = value.substring(value.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/" + hash + "\"}}}";
            texture = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } else if (!value.matches("[A-Za-z0-9+/]{100,}={0,2}")) {
            return this;
        }

        UUID uuid = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
        PlayerProfile profile = Bukkit.createProfile(uuid);
        profile.setProperty(new ProfileProperty("textures", texture));
        skullMeta.setPlayerProfile(profile);
        return this;
    }

    public PaperItemBuilder skull(OfflinePlayer identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return meta(SkullMeta.class, skullMeta -> skullMeta.setPlayerProfile(identifier.getPlayerProfile()));
    }

    public PaperItemBuilder skull(UUID identifier) {
        Objects.requireNonNull(identifier, "identifier");
        return meta(SkullMeta.class, skullMeta -> skullMeta.setPlayerProfile(Bukkit.createProfile(identifier)));
    }

    public PaperItemBuilder placeholder(PlaceholderComponent placeholder) {
        Component currentName = this.meta.displayName();
        if (currentName != null) {
            this.name(placeholder.translate(currentName));
        }
        List<Component> currentLore = this.meta.lore();
        if (currentLore != null) {
            this.lore(placeholder.translate(currentLore));
        }
        return this;
    }

    public PaperItemBuilder attribute(Material material, EquipmentSlot slot) {
        this.meta.setAttributeModifiers(material.getDefaultAttributeModifiers(slot));
        return this;
    }

    public PaperItemBuilder loreCustom(String key, Component replacer) {
        return loreCustom(key, replacer, null);
    }

    public PaperItemBuilder loreCustom(String key, Component replacer, @Nullable PlaceholderComponent placeholder) {
        return loreCustom(key, List.of(replacer), placeholder);
    }

    public PaperItemBuilder loreCustom(String key, List<Component> replacer) {
        return loreCustom(key, replacer, null);
    }

    public PaperItemBuilder loreCustom(String key, List<Component> replacer, @Nullable PlaceholderComponent placeholder) {
        List<Component> lore = new ArrayList<>();
        if (placeholder != null) {
            replacer = placeholder.translate(replacer);
        }
        List<Component> currentLore = this.meta.lore();
        if (currentLore != null) {
            for (Component line : currentLore) {
                if (PlainTextComponentSerializer.plainText().serialize(line).contains(key)) {
                    lore.addAll(replacer);
                    continue;
                }
                lore.add(line);
            }
        }
        this.lore(lore);
        return this;
    }

    @Nullable
    public static ItemBuilder fromConfig(Config config, String path) {
        return ItemBuilder.fromConfig(config, path);
    }

    @Nullable
    public static ItemBuilder fromConfig(Config config, String path, @Nullable Placeholder placeholder) {
        return ItemBuilder.fromConfig(config, path, placeholder);
    }

    @Nullable
    public static ItemBuilder fromConfig(@Nullable Player player, Config config, String path) {
        return ItemBuilder.fromConfig(player, config, path);
    }

    @Nullable
    public static ItemBuilder fromConfig(@Nullable Player player, Config config, String path, @Nullable Placeholder placeholder) {
        return ItemBuilder.fromConfig(player, config, path, placeholder);
    }

    @Nullable
    public static ItemBuilder fromConfig(FileConfiguration config, String path) {
        return ItemBuilder.fromConfig(config, path);
    }

    @Nullable
    public static ItemBuilder fromConfig(FileConfiguration config, String path, @Nullable Placeholder placeholder) {
        return ItemBuilder.fromConfig(config, path, placeholder);
    }

    @Nullable
    public static ItemBuilder fromConfig(@Nullable Player player, FileConfiguration config, String path) {
        return ItemBuilder.fromConfig(player, config, path);
    }

    @Nullable
    public static ItemBuilder fromConfig(@Nullable Player player, FileConfiguration config, String path, @Nullable Placeholder placeholder) {
        return ItemBuilder.fromConfig(player, config, path, placeholder);
    }

    @Nullable
    public static ItemBuilder fromConfig(ConfigurationSection section) {
        return ItemBuilder.fromConfig(section);
    }

    @Nullable
    public static ItemBuilder fromConfig(ConfigurationSection section, @Nullable Placeholder placeholder) {
        return ItemBuilder.fromConfig(section, placeholder);
    }

    @Nullable
    public static ItemBuilder fromConfig(@Nullable Player player, ConfigurationSection section) {
        return ItemBuilder.fromConfig(player, section);
    }

    @Nullable
    public static ItemBuilder fromConfig(@Nullable Player player, ConfigurationSection section, @Nullable Placeholder placeholder) {
        return ItemBuilder.fromConfig(player, section, placeholder);
    }

    public static ItemBuilder retrieveItemBuilder(String materialString) {
        return ItemBuilder.retrieveItemBuilder(materialString);
    }

    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        return this.item;
    }

    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        return ItemBuilder.isVersionAtLeast(major, minor, patch);
    }
}
