package com.muhammaddaffa.mdlib.core.task;

public interface TaskHandle {

    void cancel();

    boolean isCancelled();

    /**
     * The platform task object behind this handle, such as a BukkitTask or a Velocity ScheduledTask.
     */
    Object getRaw();

}
