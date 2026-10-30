package com.muhammaddaffa.mdlib.utils;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.task.provider.VelocityProvider;

public class Executor {

    public static HandleTask async(Runnable runnable) {
        return provider().async(runnable);
    }

    public static HandleTask asyncLater(long delay, Runnable runnable) {
        return provider().asyncLater(delay, runnable);
    }

    public static HandleTask asyncTimer(long delay, long runEvery, Runnable runnable) {
        return provider().asyncTimer(delay, runEvery, runnable);
    }

    private static VelocityProvider provider() {
        return MDLib.getTaskProvider();
    }

}
