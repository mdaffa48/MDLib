package com.muhammaddaffa.mdlib.task.provider;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.ExecutorProvider;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaProvider implements ExecutorProvider {

    @Override
    public HandleTask sync(Runnable runnable) {
        return new HandleTask(Bukkit.getGlobalRegionScheduler().run(MDLib.instance(), task -> runnable.run()));
    }

    @Override
    public HandleTask syncLater(long delay, Runnable runnable) {
        return new HandleTask(Bukkit.getGlobalRegionScheduler().runDelayed(MDLib.instance(), task -> runnable.run(), delay));
    }

    @Override
    public HandleTask syncTimer(long delay, long runEvery, Runnable runnable) {
        return new HandleTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(MDLib.instance(), task -> runnable.run(), delay, runEvery));
    }

    @Override
    public HandleTask async(Runnable runnable) {
        return new HandleTask(Bukkit.getAsyncScheduler().runNow(MDLib.instance(), task -> runnable.run()));
    }

    @Override
    public HandleTask asyncLater(long delay, Runnable runnable) {
        return new HandleTask(Bukkit.getAsyncScheduler().runDelayed(MDLib.instance(), task -> runnable.run(), delay, TimeUnit.MILLISECONDS));
    }

    @Override
    public HandleTask asyncTimer(long delay, long runEvery, Runnable runnable) {
        return new HandleTask(Bukkit.getAsyncScheduler().runAtFixedRate(MDLib.instance(), task -> runnable.run(), delay, runEvery, TimeUnit.MILLISECONDS));
    }

    // Optional Method

    @Override
    public HandleTask region(World world, int chunkX, int chunkZ, Runnable runnable) {
        return new HandleTask(Bukkit.getRegionScheduler().run(MDLib.instance(), world, chunkX, chunkZ, task -> runnable.run()));
    }

    @Override
    public HandleTask regionLater(World world, int chunkX, int chunkZ, long delay, Runnable runnable) {
        return new HandleTask(Bukkit.getRegionScheduler().runDelayed(MDLib.instance(), world, chunkX, chunkZ, task -> runnable.run(), delay));
    }

    @Override
    public HandleTask regionTimer(World world, int chunkX, int chunkZ, long delay, long runEvery, Runnable runnable) {
        return new HandleTask(Bukkit.getRegionScheduler().runAtFixedRate(MDLib.instance(), world, chunkX, chunkZ, task -> runnable.run(), delay, runEvery));
    }

    @Override
    public HandleTask entity(Consumer<ScheduledTask> consumer, Runnable runnable) {
        return new HandleTask(Bukkit.getGlobalRegionScheduler().run(MDLib.instance(), task -> {
            consumer.accept(task);
            runnable.run();
        }));
    }

    @Override
    public HandleTask entityLater(Consumer<ScheduledTask> consumer, long delay, Runnable runnable) {
        return new HandleTask(Bukkit.getGlobalRegionScheduler().runDelayed(MDLib.instance(), task -> {
            consumer.accept(task);
            runnable.run();
        }, delay));
    }

    @Override
    public HandleTask entityTimer(Consumer<ScheduledTask> consumer, long delay, long runEvery, Runnable runnable) {
        return new HandleTask(Bukkit.getGlobalRegionScheduler().runAtFixedRate(MDLib.instance(), task -> {
            consumer.accept(task);
            runnable.run();
        }, delay, runEvery));
    }
}
