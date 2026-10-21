package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.task.ExecutorProvider;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import org.bukkit.entity.Entity;

public class Executor {

    public static HandleTask sync(Runnable runnable) {
        return provider().sync(runnable);
    }

    public static HandleTask syncLater(long delay, Runnable runnable) {
        return provider().syncLater(delay, runnable);
    }

    public static HandleTask syncTimer(long delay, long runEvery, Runnable runnable) {
        return provider().syncTimer(delay, runEvery, runnable);
    }

    public static HandleTask async(Runnable runnable) {
        return provider().async(runnable);
    }

    public static HandleTask asyncLater(long delay, Runnable runnable) {
        return provider().asyncLater(delay, runnable);
    }

    public static HandleTask asyncTimer(long delay, long runEvery, Runnable runnable) {
        return provider().asyncTimer(delay, runEvery, runnable);
    }

    public static HandleTask entity(Entity entity, Runnable runnable) {
        return provider().entity(entity, runnable);
    }

    public static HandleTask entityLater(Entity entity, long delay, Runnable runnable) {
        return provider().entityLater(entity, delay, runnable);
    }

    public static HandleTask entityTimer(Entity entity, long delay, long runEvery, Runnable runnable) {
        return provider().entityTimer(entity, delay, runEvery, runnable);
    }

    private static ExecutorProvider provider() {
        return ExecutorManager.getProvider();
    }
    
}
