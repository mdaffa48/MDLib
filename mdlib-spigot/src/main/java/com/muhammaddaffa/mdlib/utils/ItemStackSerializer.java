package com.muhammaddaffa.mdlib.utils;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ItemStackSerializer {

    @Nullable
    public static ItemStack readItem(String source) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decode(source));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item;

            // Read the serialized inventory
            item = (ItemStack) dataInput.readObject();

            dataInput.close();
            return item;
        } catch (Exception ex) {
            return null;
        }
    }

    public static String writeItem(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            // Save every element
            dataOutput.writeObject(item);

            // Serialize that array
            dataOutput.close();
            return new String(Base64Coder.encode(outputStream.toByteArray()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stack.", e);
        }
    }

    /**
     * Serializes an array of ItemStacks into a Base64 string.
     *
     * @param items The array of ItemStacks to serialize.
     * @return A Base64 string representation of the items, or an empty string if an error occurs.
     */
    public static String writeArray(ItemStack[] items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());

        } catch (Exception e) {
            // Log or handle exception appropriately
            return "";
        }
    }

    /**
     * Deserializes a Base64 string into an array of ItemStacks.
     *
     * @param source The Base64 string to deserialize.
     * @return The array of ItemStacks, or null if the source is invalid.
     */
    @Nullable
    public static ItemStack[] readArray(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];

            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            return items;

        } catch (Exception e) {
            // Log or handle exception appropriately
            return null;
        }
    }

    /**
     * Serializes an Inventory into a Base64 string.
     *
     * @param inventory The Inventory to serialize.
     * @return A Base64 string representation of the inventory.
     */
    public static String writeInventory(Inventory inventory) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(inventory.getSize());

            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());

        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     * Deserializes a Base64 string into an Inventory.
     *
     * @param source The Base64 string to deserialize.
     * @return The deserialized Inventory.
     */
    @Nullable
    public static Inventory readInventory(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            int size = dataInput.readInt();
            Inventory inventory = Bukkit.createInventory(null, size);

            for (int i = 0; i < size; i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }
            return inventory;

        } catch (Exception e) {
            return null;
        }
    }

}
