package com.muhammaddaffa.mdlib.task.provider;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.ExecutorProvider;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;

public class BukkitProvider implements ExecutorProvider {

    @Override
    public HandleTask sync(Runnable runnable) {
        return new HandleTask(Bukkit.getScheduler().runTask(MDLib.instance(), runnable));
    }

    @Override
    public HandleTask syncLater(long delay, Runnable runnable) {
        return new HandleTask(Bukkit.getScheduler().runTaskLater(MDLib.instance(), runnable, delay));
    }

    @Override
    public HandleTask syncTimer(long delay, long runEvery, Runnable runnable) {
        return new HandleTask(Bukkit.getScheduler().runTaskTimer(MDLib.instance(), runnable, delay, runEvery));
    }

    @Override
    public HandleTask async(Runnable runnable) {
        return new HandleTask(Bukkit.getScheduler().runTaskAsynchronously(MDLib.instance(), runnable));
    }

    @Override
    public HandleTask asyncLater(long delay, Runnable runnable, TimeUnit timeUnit) {
        return new HandleTask(Bukkit.getScheduler().runTaskLaterAsynchronously(MDLib.instance(), runnable, delay));
    }

    @Override
    public HandleTask asyncTimer(long delay, long runEvery, Runnable runnable, TimeUnit timeUnit) {
        return new HandleTask(Bukkit.getScheduler().runTaskTimerAsynchronously(MDLib.instance(), runnable, delay, runEvery));
    }
}
