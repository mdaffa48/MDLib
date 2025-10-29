package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FoliaExecutor {

    /**
     * Run a task on the main thread.
     *
     * @param runnable It needs to accept a {@link ScheduledTask} as a parameter.
     * @return ScheduledTask
     *
     * Example:
     * ```java
     * FoliaExecutor.runTask(task -> {
     *     // Your code here
     * });
     * ```
     */
    public ScheduledTask runTask(Consumer<ScheduledTask> runnable) {
        return Bukkit.getGlobalRegionScheduler().run(MDLib.instance(), runnable);
    }

    /**
     * Run a task on the main thread after a delay.
     *
     * @param runnable It needs to accept a {@link ScheduledTask} as a parameter.
     * @param delay Delay in ticks.
     * @return ScheduledTask
     *
     * Example:
     * ```java
     * FoliaExecutor.runTaskLater(task -> {
     *     // Your code here
     * }, 20);
     * ```
     */
    public ScheduledTask runTaskLater(Consumer<ScheduledTask> runnable, long delay) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(MDLib.instance(), runnable, delay);
    }

    /**
     * Run a task on the main thread after a delay and repeat it.
     *
     * @param runnable It needs to accept a {@link ScheduledTask} as a parameter.
     * @param delay Delay in ticks.
     * @param period Period in ticks.
     * @return ScheduledTask
     *
     * Example:
     * ```java
     * FoliaExecutor.runTaskTimer(task -> {
     *     // Your code here
     * }, 20, 20);
     * ```
     */
    public ScheduledTask runTaskTimer(Consumer<ScheduledTask> runnable, long delay, long period) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(MDLib.instance(), runnable, delay, period);
    }

    /**
     * Run a task asynchronously.
     *
     * @param runnable It needs to accept a {@link ScheduledTask} as a parameter.
     * @return ScheduledTask
     *
     * Example:
     * ```java
     * FoliaExecutor.runTaskAsynchronously(task -> {
     *     // Your code here
     * });
     * ```
     */
    public ScheduledTask runTaskAsynchronously(Consumer<ScheduledTask> runnable) {
        return Bukkit.getAsyncScheduler().runNow(MDLib.instance(), runnable);
    }

    /**
     * Run a task asynchronously after a delay.
     *
     * @param runnable It needs to accept a {@link ScheduledTask} as a parameter.
     * @param delay Delay in ticks.
     * @param timeUnit Time unit.
     * @return ScheduledTask
     *
     * Example:
     * ```java
     * FoliaExecutor.runTaskLaterAsynchronously(task -> {
     *     // Your code here
     * }, 20, TimeUnit.MILLISECONDS);
     * ```
     */
    public ScheduledTask runTaskLaterAsynchronously(Consumer<ScheduledTask> runnable, long delay, TimeUnit timeUnit) {
        return Bukkit.getAsyncScheduler().runDelayed(MDLib.instance(), runnable, delay, timeUnit);
    }

    /**
     * Run a task asynchronously after a delay and repeat it.
     *
     * @param runnable It needs to accept a {@link ScheduledTask} as a parameter.
     * @param delay Delay in ticks.
     * @param period Period in ticks.
     * @param timeUnit Time unit.
     * @return ScheduledTask
     *
     * Example:
     * ```java
     * FoliaExecutor.runTaskTimerAsynchronously(task -> {
     *     // Your code here
     * }, 20, 20, TimeUnit.MILLISECONDS);
     * ```
     */
    public ScheduledTask runTaskTimerAsynchronously(Consumer<ScheduledTask> runnable, long delay, long period, TimeUnit timeUnit) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(MDLib.instance(), runnable, delay, period, timeUnit);
    }
}
