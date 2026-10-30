package com.muhammaddaffa.mdlib.core.task;

public interface AsyncTaskProvider {

    /**
     * Run a task asynchronously.
     */
    TaskHandle async(Runnable runnable);

    /**
     * Run a task asynchronously after a delay in ticks (1 tick = 50 ms).
     */
    TaskHandle asyncLater(long delay, Runnable runnable);

    /**
     * Run a task asynchronously after a delay and repeat it, both in ticks (1 tick = 50 ms).
     */
    TaskHandle asyncTimer(long delay, long runEvery, Runnable runnable);

}
