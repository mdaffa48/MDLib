package com.muhammaddaffa.mdlib.task.handleTask;

import com.muhammaddaffa.mdlib.core.task.TaskHandle;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.TaskStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class HandleTask implements TaskHandle {

    private static final Set<ScheduledTask> tasks = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    private final ScheduledTask task;

    public HandleTask(ScheduledTask task) {
        this.task = task;
        if (task != null) {
            tasks.add(task);
        }
    }

    public void cancel() {
        if (task != null) {
            task.cancel();
            tasks.remove(task);
        }
    }

    public boolean isCancelled() {
        return task == null || task.status() == TaskStatus.CANCELLED;
    }

    public Object getRaw() {
        return task;
    }

    /**
     * Cancels every task MDLib scheduled, leaving the plugin's own tasks running.
     */
    public static void cancelAll() {
        ArrayList<ScheduledTask> pending;
        synchronized (tasks) {
            pending = new ArrayList<>(tasks);
            tasks.clear();
        }
        for (ScheduledTask task : pending) {
            task.cancel();
        }
    }
}
