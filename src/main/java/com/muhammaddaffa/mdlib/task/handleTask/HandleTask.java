package com.muhammaddaffa.mdlib.task.handleTask;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

public class HandleTask {

    private ScheduledTask foliaTask;
    private BukkitTask bukkitTask;

    public HandleTask(ScheduledTask foliaTask) {
        this.foliaTask = foliaTask;
    }

    public HandleTask(BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }

    public void cancel() {
        if (foliaTask != null) {
            foliaTask.cancel();
        } else {
            bukkitTask.cancel();
        }
    }

    public boolean isCancelled() {
        if (foliaTask != null) {
            return foliaTask.isCancelled();
        } else {
            return bukkitTask.isCancelled();
        }
    }

    public Object getRaw() {
        return foliaTask != null ? foliaTask : bukkitTask;
    }
}
