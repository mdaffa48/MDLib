package com.muhammaddaffa.mdlib.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtils {

    public static String serialize(Location location) {
        return location.getX() + ";" +
                location.getY() + ";" +
                location.getZ() + ";" +
                location.getYaw() + ";" +
                location.getPitch() + ";" +
                location.getWorld().getName();
    }

    public static Location deserialize(String string) {//Converts String -> Location
        String[] parts = string.split(";"); //If you changed the semicolon you must change it here too
        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        float yaw = (float) Double.parseDouble(parts[3]);
        float pitch = (float) Double.parseDouble(parts[4]);
        World w = Bukkit.getServer().getWorld(parts[5]);
        return new Location(w, x, y, z, yaw, pitch); //can return null if the world no longer exists
    }

}
