package com.muhammaddaffa.mdlib.utils;

import com.cryptomorin.xseries.profiles.builder.XSkull;
import com.cryptomorin.xseries.profiles.objects.ProfileInputType;
import com.cryptomorin.xseries.profiles.objects.Profileable;
import com.nexomc.nexo.api.NexoItems;
import dev.lone.itemsadder.api.CustomStack;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import me.arcaniax.hdb.api.HeadDatabaseAPI;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        this.item = this.item.withType(material);
        this.meta = this.item.getItemMeta();
        return this;
    }

    public PaperItemBuilder durability(int durability) {
        this.item.setData(DataComponentTypes.DAMAGE, durability);
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
        this.item.addEnchantment(enchantment, level);
        return this;
    }

    public PaperItemBuilder removeEnchant(Enchantment enchantment) {
        this.item.removeEnchantment(enchantment);
        return this;
    }

    public PaperItemBuilder removeEnchants() {
        this.item.getEnchantments().keySet().forEach(this.meta::removeEnchant);
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
        this.item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        return this;
    }

    public PaperItemBuilder addLore(Component line) {
        List<Component> lore = this.item.lore();

        if (lore == null) {
            return lore(line);
        }

        lore.add(line);
        return lore(lore);
    }

    public PaperItemBuilder addLore(Component... lines) {
        return addLore(Arrays.asList(lines));
    }

    public PaperItemBuilder addLore(List<Component> lines) {
        List<Component> lore = this.item.lore();

        if (lore == null) {
            return lore(lines);
        }

        lore.addAll(lines);
        return lore(lore);
    }

    public PaperItemBuilder flags(ItemFlag... flags) {
        this.item.addItemFlags(flags);
        return this;
    }

    public PaperItemBuilder flags() {
        return flags(ItemFlag.values());
    }

    public PaperItemBuilder removeFlags(ItemFlag... flags) {
        this.item.removeItemFlags(flags);
        return this;
    }

    public PaperItemBuilder removeFlags() {
        return removeFlags(ItemFlag.values());
    }

    public PaperItemBuilder armorColor(Color color) {
        return meta(LeatherArmorMeta.class, m -> m.setColor(color));
    }

    @Deprecated(since = "1.21.5")
    public PaperItemBuilder customModelData(int data){
        this.meta.setCustomModelData(data);
        return this;
    }

    public PaperItemBuilder itemModel(NamespacedKey key){
        this.meta.setItemModel(key);
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, String s){
        this.item.editPersistentDataContainer(setter -> setter.set(key, PersistentDataType.STRING, s));
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Double d){
        this.item.editPersistentDataContainer(setter -> setter.set(key, PersistentDataType.DOUBLE, d));
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Float f){
        this.item.editPersistentDataContainer(setter -> setter.set(key, PersistentDataType.FLOAT, f));
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Integer i){
        this.item.editPersistentDataContainer(setter -> setter.set(key, PersistentDataType.INTEGER, i));
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Long l){
        this.item.editPersistentDataContainer(setter -> setter.set(key, PersistentDataType.LONG, l));
        return this;
    }

    public PaperItemBuilder pdc(NamespacedKey key, Byte b){
        this.item.editPersistentDataContainer(setter -> setter.set(key, PersistentDataType.BYTE, b));
        return this;
    }

    public PaperItemBuilder skull(String identifier){
        ProfileInputType input = ProfileInputType.typeOf(identifier);
        if (input != null)
            XSkull.of(this.item).profile(Profileable.of(input, identifier)).apply();
        return this;
    }

    public PaperItemBuilder skull(OfflinePlayer identifier){
        XSkull.of(this.item).profile(Profileable.of(identifier)).apply();
        return this;
    }

    public PaperItemBuilder skull(UUID identifier){
        XSkull.of(this.item).profile(Profileable.of(identifier)).apply();
        return this;
    }

    public PaperItemBuilder placeholder(PlaceholderComponent placeholder) {
        Component currentName = this.item.displayName();
        if (currentName != null) {
            this.name(placeholder.translate(currentName));
        }
        List<Component> currentLore = this.item.lore();
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
        // Check if the placeholder is not null
        if (placeholder != null) replacer = placeholder.translate(replacer);
        // Get the lore
        if (item.lore() != null) {
            for (Component line : item.lore()) {
                if (line.contains(Component.text(key))) {
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
        return fromConfig(config, path, null);
    }

    @Nullable
    public static ItemBuilder fromConfig(Config config, String path, @Nullable Placeholder placeholder) {
        return fromConfig(config.getConfig(), path, placeholder);
    }

    @Nullable
    public static ItemBuilder fromConfig(FileConfiguration config, String path) {
        return fromConfig(config, path, null);
    }

    @Nullable
    public static ItemBuilder fromConfig(FileConfiguration config, String path, @Nullable Placeholder placeholder) {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return null;
        }
        return fromConfig(section, placeholder);
    }

    @Nullable
    public static ItemBuilder fromConfig(ConfigurationSection section) {
        return fromConfig(section, null);
    }

    @Nullable
    public static ItemBuilder fromConfig(ConfigurationSection section, @Nullable Placeholder placeholder) {
        if (section == null) {
            return null;
        }
        // get all the available variables
        String materialString = section.getString("material", "BARRIER");
        if (placeholder != null) materialString = placeholder.translate(materialString);
        Integer cmd = section.get("custom-model-data") == null ? null : section.getInt("custom-model-data");
        String itemModel = section.getString("item-model") == null ? "" : section.getString("item-model");
        int amount = section.getInt("amount");
        String displayName = section.getString("display-name");
        List<String> lore = section.getStringList("lore");
        List<String> flags = section.getStringList("flags");
        List<String> enchantments = section.getStringList("enchantments");

        // Parse the placeholder on the material
        if (placeholder != null) {
            materialString = placeholder.translate(materialString);
        }

        if (isPlaceholderAPI()) {
            materialString = PlaceholderAPI.setPlaceholders(null, materialString);
            if (displayName != null) displayName = PlaceholderAPI.setPlaceholders(null, displayName);
            lore = PlaceholderAPI.setPlaceholders(null, lore);
        }

        // start building the item
        ItemBuilder builder = retrieveItemBuilder(materialString);

        // set the amount
        builder.amount(Math.max(1, amount));
        // set the cmd
        if (cmd != null && cmd != 0) {
            builder.customModelData(cmd);
        }
        // set the display name
        if (displayName != null) {
            builder.name(displayName);
        }
        // set the lore
        builder.lore(lore);
        // set the item flag
        for (String flag : flags) {
            if (isValidItemFlag(flag)) {
                builder.flags(ItemFlag.valueOf(flag));
            }
        }
        // enchantments
        for (String enchantment : enchantments) {
            String[] split = enchantment.split(";");
            Enchantment enchant = Enchantment.getByName(split[0]);
            int level = Integer.parseInt(split[1]);
            // enchant the item
            builder.enchant(enchant, level);
        }
        // set the placeholder
        if (placeholder != null) {
            builder.placeholder(placeholder);
        }

        // Try to get the color
        if (section.get("color") != null) {
            int r = section.getInt("color.r", 255);
            int g = section.getInt("color.g", 255);
            int b = section.getInt("color.b", 255);
            // Create a color object
            Color color = Color.fromRGB(r, g, b);
            // If the item is a tipped arrow, apply it
            builder.meta(PotionMeta.class, meta -> {
                meta.setColor(color);
            });
            // If the item is a leather armor meta
            builder.meta(LeatherArmorMeta.class, meta -> {
                meta.setColor(color);
            });
        }

        // set the item model
        if (itemModel != null && isVersionAtLeast(1, 21, 4)) {
            builder.itemModel(NamespacedKey.fromString(itemModel));
        } else if (itemModel != null) {
            Logger.warning("Item model is not supported on this version of Minecraft! Ignoring...");
        }

        return builder;
    }

    public static ItemBuilder retrieveItemBuilder(String materialString) {
        String[] parts  = materialString.split(";", 2);
        String id       = parts[0].trim();
        String val      = parts.length > 1 ? parts[1].trim() : "";

        if (parts.length == 2) {
            switch (id.toLowerCase()) {
                case "head":
                    return new ItemBuilder(Material.PLAYER_HEAD)
                            .skull(val);
                case "nexo":
                    if (Bukkit.getPluginManager().isPluginEnabled("Nexo")) {
                        return NexoItems
                                .optionalItemFromId(val)
                                .map(nb -> new ItemBuilder(nb.build()))
                                .orElseGet(() -> new ItemBuilder(Material.DIRT));
                    }
                    break;
                case "ia":
                    if (Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
                        CustomStack stack = CustomStack.getInstance(val);
                        if (stack != null) {
                            return new ItemBuilder(stack.getItemStack());
                        }
                    }
                    break;
                case "hdb":
                    if (Bukkit.getPluginManager().isPluginEnabled("HeadDatabase")) {
                        HeadDatabaseAPI api = new HeadDatabaseAPI();
                        ItemStack head = api.getItemHead(val);
                        if (head != null) {
                            return new ItemBuilder(head);
                        }
                    }
                    break;
            }
        }

        // if we get here nothing matched, so treat the whole string as a vanilla material:
        Material mat = Material.matchMaterial(materialString);
        return new ItemBuilder(mat == null ? Material.DIRT : mat);
    }

    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        return this.item;
    }

    private static boolean isPlaceholderAPI() {
        return Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    private static boolean isValidItemFlag(String flag) {
        try {
            ItemFlag.valueOf(flag.toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isVersionAtLeast(int major, int minor, int patch) {
        String version = Bukkit.getBukkitVersion().split("-")[0];
        String[] parts = version.split("\\.");

        try {
            int maj = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            int pat = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

            if (maj != major) return maj > major;
            if (min != minor) return min > minor;
            return pat >= patch;
        } catch (NumberFormatException e) {
            return false; // fallback
        }
    }

}
