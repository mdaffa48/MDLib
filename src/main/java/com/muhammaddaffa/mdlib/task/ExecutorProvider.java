package com.muhammaddaffa.mdlib.task;

import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.function.Consumer;

public interface ExecutorProvider {

    /**
     * Run a task on the main thread.
     *
     * @param runnable
     * @return
     */
    HandleTask sync(Runnable runnable);

    /**
     * Run a task on the main thread after a delay.
     *
     * @param delay
     * @param runnable
     */
    HandleTask syncLater(long delay, Runnable runnable);

    /**
     * Run a task on the main thread after a delay and repeat it.
     *
     * @param delay
     * @param runEvery
     * @param runnable
     */
    HandleTask syncTimer(long delay, long runEvery, Runnable runnable);

    /**
     * Run a task asynchronously.
     *
     * @param runnable
     */
    HandleTask async(Runnable runnable);

    /**
     * Run a task asynchronously after a delay.
     *
     * @param delay
     * @param runnable
     */
    HandleTask asyncLater(long delay, Runnable runnable);

    /**
     * Run a task asynchronously after a delay and repeat it.
     *
     * @param delay
     * @param runEvery
     * @param runnable
     */
    HandleTask asyncTimer(long delay, long runEvery, Runnable runnable);

    /**
     * Run a task on the region's thread.
     *
     * @param world
     * @param chunkX
     * @param chunkZ
     * @param runnable
     * @return
     */
    default HandleTask region(World world, int chunkX, int chunkZ, Runnable runnable) {
        return sync(runnable);
    }

    /**
     * Run a task on the region's thread after a delay.
     *
     * @param world
     * @param chunkX
     * @param chunkZ
     * @param delay
     * @param runnable
     * @return
     */
    default HandleTask regionLater(World world, int chunkX, int chunkZ, long delay, Runnable runnable) {
        return syncLater(delay, runnable);
    }

    /**
     * Run a task on the region's thread after a delay and repeat it.
     *
     * @param world
     * @param chunkX
     * @param chunkZ
     * @param delay
     * @param runEvery
     * @param runnable
     * @return
     */
    default HandleTask regionTimer(World world, int chunkX, int chunkZ, long delay, long runEvery, Runnable runnable) {
        return syncTimer(delay, runEvery, runnable);
    }

    /**
     * Run a task on the entity's thread.
     *
     * @param consumer
     * @param runnable
     * @return
     */
    default HandleTask entity(Consumer<ScheduledTask> consumer, Runnable runnable) {
        return sync(runnable);
    }

    /**
     * Run a task on the entity's thread after a delay.
     *
     * @param consumer
     * @param delay
     * @param runnable
     * @return
     */
    default HandleTask entityLater(Consumer<ScheduledTask> consumer, long delay, Runnable runnable) {
        return syncLater(delay, runnable);
    }

    /**
     * Run a task on the entity's thread after a delay and repeat it.
     *
     * @param consumer
     * @param delay
     * @param runEvery
     * @param runnable
     * @return
     */
    default HandleTask entityTimer(Consumer<ScheduledTask> consumer, long delay, long runEvery, Runnable runnable) {
        return syncTimer(delay, runEvery, runnable);
    }
}
