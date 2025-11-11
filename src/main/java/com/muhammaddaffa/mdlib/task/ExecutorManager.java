package com.muhammaddaffa.mdlib.task;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.task.provider.BukkitProvider;
import com.muhammaddaffa.mdlib.task.provider.FoliaProvider;

public class ExecutorManager {

    private static ExecutorProvider provider;

    public static void init() {
        provider = isFolia() ? new FoliaProvider() : new BukkitProvider();
    }

    public static ExecutorProvider getProvider() {
        if (provider == null) {
            init();
        }
        return provider;
    }

    public static boolean isFolia() {
        return MDLib.isFolia();
    }

    public static boolean isPaper() {
        return MDLib.isPaper();
    }
}
