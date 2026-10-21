package com.muhammaddaffa.mdlib.task.handleTask;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class HandleTask {

    private static final Set<Object> tasks = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    private ScheduledTask foliaTask;
    private BukkitTask bukkitTask;

    public HandleTask(ScheduledTask foliaTask) {
        this.foliaTask = track(foliaTask);
    }

    public HandleTask(BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
        if (bukkitTask != null) {
            tasks.add(bukkitTask);
        }
    }

    public void cancel() {
        if (foliaTask != null) {
            foliaTask.cancel();
        } else if (bukkitTask != null) {
            bukkitTask.cancel();
        }
        tasks.remove(getRaw());
    }

    public boolean isCancelled() {
        if (foliaTask != null) {
            return foliaTask.isCancelled();
        } else {
            return bukkitTask == null || bukkitTask.isCancelled();
        }
    }

    public Object getRaw() {
        return foliaTask != null ? foliaTask : bukkitTask;
    }

    public static ScheduledTask track(ScheduledTask task) {
        if (task != null) {
            tasks.add(task);
        }
        return task;
    }

    public static void cancelAll() {
        ArrayList<Object> pending;
        synchronized (tasks) {
            pending = new ArrayList<>(tasks);
            tasks.clear();
        }
        for (Object task : pending) {
            if (task instanceof ScheduledTask scheduledTask) {
                scheduledTask.cancel();
            } else if (task instanceof BukkitTask bukkitTask) {
                bukkitTask.cancel();
            }
        }
    }
}
