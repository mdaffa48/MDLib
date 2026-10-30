package com.muhammaddaffa.mdlib.task.provider;

import com.muhammaddaffa.mdlib.core.task.AsyncTaskProvider;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.concurrent.TimeUnit;

public class VelocityProvider implements AsyncTaskProvider {

    private final ProxyServer server;
    private final Object plugin;

    public VelocityProvider(ProxyServer server, Object plugin) {
        this.server = server;
        this.plugin = plugin;
    }

    @Override
    public HandleTask async(Runnable runnable) {
        return new HandleTask(server.getScheduler().buildTask(plugin, runnable).schedule());
    }

    @Override
    public HandleTask asyncLater(long delay, Runnable runnable) {
        return new HandleTask(server.getScheduler().buildTask(plugin, runnable)
                .delay(millis(delay), TimeUnit.MILLISECONDS)
                .schedule());
    }

    @Override
    public HandleTask asyncTimer(long delay, long runEvery, Runnable runnable) {
        return new HandleTask(server.getScheduler().buildTask(plugin, runnable)
                .delay(millis(delay), TimeUnit.MILLISECONDS)
                .repeat(millis(runEvery), TimeUnit.MILLISECONDS)
                .schedule());
    }

    private static long millis(long ticks) {
        return Math.multiplyExact(ticks, 50L);
    }
}
