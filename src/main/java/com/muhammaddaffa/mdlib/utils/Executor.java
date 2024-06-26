package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class Executor {

    public static BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(MDLib.instance(), runnable);
    }

    public static BukkitTask syncLater(long delay, Runnable runnable) {
        return Bukkit.getScheduler().runTaskLater(MDLib.instance(), runnable, delay);
    }

    public static BukkitTask syncTimer(long delay, long runEvery, Runnable runnable) {
        return Bukkit.getScheduler().runTaskTimer(MDLib.instance(), runnable, delay, runEvery);
    }

    public static BukkitTask async(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(MDLib.instance(), runnable);
    }

    public static BukkitTask asyncLater(long delay, Runnable runnable) {
        return Bukkit.getScheduler().runTaskLaterAsynchronously(MDLib.instance(), runnable, delay);
    }

    public static BukkitTask asyncTimer(long delay, long runEvery, Runnable runnable) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(MDLib.instance(), runnable, delay, runEvery);
    }
    
}
