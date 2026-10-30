package com.muhammaddaffa.mdlib.task;

import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.task.provider.BungeeProvider;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchedulerTest {

    private final Plugin plugin = mock(Plugin.class);
    private final TaskScheduler scheduler = mock(TaskScheduler.class);

    @BeforeEach
    void setUp() {
        ProxyServer proxy = mock(ProxyServer.class);
        when(plugin.getProxy()).thenReturn(proxy);
        when(proxy.getScheduler()).thenReturn(scheduler);
    }

    @AfterEach
    void tearDown() {
        HandleTask.cancelAll();
    }

    @Test
    void ticksBecomeMillisecondsOnTheProxyScheduler() {
        BungeeProvider provider = new BungeeProvider(plugin);
        Runnable runnable = () -> {};
        ScheduledTask now = mock(ScheduledTask.class);
        ScheduledTask later = mock(ScheduledTask.class);
        ScheduledTask timer = mock(ScheduledTask.class);
        when(scheduler.runAsync(plugin, runnable)).thenReturn(now);
        when(scheduler.schedule(plugin, runnable, 150L, TimeUnit.MILLISECONDS)).thenReturn(later);
        when(scheduler.schedule(plugin, runnable, 100L, 200L, TimeUnit.MILLISECONDS)).thenReturn(timer);

        assertSame(now, provider.async(runnable).getRaw());
        assertSame(later, provider.asyncLater(3, runnable).getRaw());
        assertSame(timer, provider.asyncTimer(2, 4, runnable).getRaw());
        assertThrows(ArithmeticException.class, () -> provider.asyncLater(Long.MAX_VALUE, runnable));
        assertThrows(ArithmeticException.class, () -> provider.asyncTimer(1, Long.MAX_VALUE / 10, runnable));
    }

    @Test
    void cancelAllOnlyCancelsTasksMDLibCreated() {
        ScheduledTask owned = mock(ScheduledTask.class);
        ScheduledTask cancelled = mock(ScheduledTask.class);
        ScheduledTask host = mock(ScheduledTask.class);
        HandleTask handle = new HandleTask(owned);
        HandleTask early = new HandleTask(cancelled);

        assertFalse(handle.isCancelled());
        assertSame(owned, handle.getRaw());
        early.cancel();
        assertTrue(early.isCancelled());
        verify(cancelled).cancel();

        HandleTask.cancelAll();

        assertTrue(handle.isCancelled());
        verify(owned).cancel();
        verify(cancelled, times(1)).cancel();
        verifyNoInteractions(host);
        verify(scheduler, never()).cancel(any(Plugin.class));

        HandleTask.cancelAll();
        verify(owned, times(1)).cancel();
    }

}
