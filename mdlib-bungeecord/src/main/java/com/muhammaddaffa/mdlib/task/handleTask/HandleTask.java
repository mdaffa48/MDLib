package com.muhammaddaffa.mdlib.task.handleTask;

import com.muhammaddaffa.mdlib.core.task.TaskHandle;
import net.md_5.bungee.api.scheduler.ScheduledTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class HandleTask implements TaskHandle {

    // Raw tasks MDLib scheduled, weakly held like on Spigot, each mapped to its handle's cancelled flag.
    private static final Map<ScheduledTask, AtomicBoolean> tasks = Collections.synchronizedMap(new WeakHashMap<>());

    private final ScheduledTask task;
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public HandleTask(ScheduledTask task) {
        this.task = task;
        if (task != null) {
            tasks.put(task, cancelled);
        }
    }

    public void cancel() {
        cancelled.set(true);
        if (task != null) {
            task.cancel();
            tasks.remove(task);
        }
    }

    /**
     * BungeeCord's ScheduledTask has no cancelled state, so this is true once {@link #cancel()} or
     * {@link #cancelAll()} cancelled the task.
     */
    public boolean isCancelled() {
        return task == null || cancelled.get();
    }

    public Object getRaw() {
        return task;
    }

    public static void cancelAll() {
        Map<ScheduledTask, AtomicBoolean> pending;
        synchronized (tasks) {
            pending = new HashMap<>(tasks);
            tasks.clear();
        }
        pending.forEach((task, cancelled) -> {
            cancelled.set(true);
            task.cancel();
        });
    }
}
