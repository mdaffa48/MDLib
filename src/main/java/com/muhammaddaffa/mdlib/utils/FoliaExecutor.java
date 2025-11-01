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
     * <p><b>Example:</b></p>
     *      * <pre>{@code
     *      * FoliaExecutor.runTask(task -> {
     *      *     // Your code here
     *      * });
     *      * }</pre>
     */
    public static ScheduledTask runTask(Consumer<ScheduledTask> runnable) {
        return Bukkit.getGlobalRegionScheduler().run(MDLib.instance(), runnable);
    }

    /**
     * Runs a task on the main server thread after a specified delay.
     *
     * @param runnable the task to run; it accepts a {@link ScheduledTask} as its parameter
     * @param delay the delay before execution, in ticks
     * @return the {@link ScheduledTask} representing the scheduled task
     *
     * <p><b>Example:</b></p>
     * <pre>{@code
     * FoliaExecutor.runTaskLater(task -> {
     *     // Your code here
     * }, 20);
     * }</pre>
     */
    public static ScheduledTask runTaskLater(Consumer<ScheduledTask> runnable, long delay) {
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
     * <p><b>Example:</b></p>
     *      * <pre>{@code
     *      * FoliaExecutor.runTaskTimer(task -> {
     *      *     // Your code here
     *      * }, 20, 20);
     *      * }</pre>
     */
    public static ScheduledTask runTaskTimer(Consumer<ScheduledTask> runnable, long delay, long period) {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(MDLib.instance(), runnable, delay, period);
    }

    /**
     * Run a task asynchronously.
     *
     * @param runnable It needs to accept a {@link ScheduledTask} as a parameter.
     * @return ScheduledTask
     *
     * <p><b>Example:</b></p>
     *      * <pre>{@code
     *      * FoliaExecutor.runTaskAsynchronously(task -> {
     *      *     // Your code here
     *      * });
     *      * }</pre>
     */
    public static ScheduledTask runTaskAsynchronously(Consumer<ScheduledTask> runnable) {
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
     * <p><b>Example:</b></p>
     *      * <pre>{@code
     *      * FoliaExecutor.runTaskLaterAsynchronously(task -> {
     *      *     // Your code here
     *      * }, 20, TimeUnit.MILLISECONDS);
     *      * }</pre>
     */
    public static ScheduledTask runTaskLaterAsynchronously(Consumer<ScheduledTask> runnable, long delay, TimeUnit timeUnit) {
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
     * <p><b>Example:</b></p>
     *      * <pre>{@code
     *      * FoliaExecutor.runTaskTimerAsynchronously(task -> {
     *      *     // Your code here
     *      * }, 20, 20, TimeUnit.MILLISECONDS);
     *      * }</pre>
     */
    public static ScheduledTask runTaskTimerAsynchronously(Consumer<ScheduledTask> runnable, long delay, long period, TimeUnit timeUnit) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(MDLib.instance(), runnable, delay, period, timeUnit);
    }
}
