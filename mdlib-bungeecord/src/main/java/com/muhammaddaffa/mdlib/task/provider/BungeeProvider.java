package com.muhammaddaffa.mdlib.task.provider;

import com.muhammaddaffa.mdlib.core.task.AsyncTaskProvider;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.TaskScheduler;

import java.util.concurrent.TimeUnit;

public class BungeeProvider implements AsyncTaskProvider {

    private final Plugin plugin;

    public BungeeProvider(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public HandleTask async(Runnable runnable) {
        return new HandleTask(scheduler().runAsync(plugin, runnable));
    }

    @Override
    public HandleTask asyncLater(long delay, Runnable runnable) {
        return new HandleTask(scheduler().schedule(plugin, runnable, millis(delay), TimeUnit.MILLISECONDS));
    }

    @Override
    public HandleTask asyncTimer(long delay, long runEvery, Runnable runnable) {
        return new HandleTask(scheduler().schedule(plugin, runnable, millis(delay), millis(runEvery), TimeUnit.MILLISECONDS));
    }

    private TaskScheduler scheduler() {
        return plugin.getProxy().getScheduler();
    }

    private static long millis(long ticks) {
        return Math.multiplyExact(ticks, 50L);
    }
}
