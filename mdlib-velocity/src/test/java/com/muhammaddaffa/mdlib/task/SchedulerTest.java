package com.muhammaddaffa.mdlib.task;

import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.task.provider.VelocityProvider;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import com.velocitypowered.api.scheduler.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SchedulerTest {

    private final Object plugin = new Object();
    private final Runnable runnable = () -> {};
    private Scheduler scheduler;
    private Scheduler.TaskBuilder builder;
    private ScheduledTask task;
    private VelocityProvider provider;

    @BeforeEach
    void setUp() {
        ProxyServer server = mock(ProxyServer.class);
        scheduler = mock(Scheduler.class);
        builder = mock(Scheduler.TaskBuilder.class, RETURNS_SELF);
        task = mock(ScheduledTask.class);
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.buildTask(eq(plugin), any(Runnable.class))).thenReturn(builder);
        when(builder.schedule()).thenReturn(task);
        provider = new VelocityProvider(server, plugin);
    }

    @AfterEach
    void tearDown() {
        HandleTask.cancelAll();
    }

    @Test
    void asyncSchedulesWithoutDelayOrRepeat() {
        HandleTask handle = provider.async(runnable);

        verify(scheduler).buildTask(plugin, runnable);
        verify(builder, never()).delay(anyLong(), any(TimeUnit.class));
        verify(builder, never()).repeat(anyLong(), any(TimeUnit.class));
        assertSame(task, handle.getRaw());
    }

    @Test
    void asyncLaterConvertsTicksToMillis() {
        provider.asyncLater(3, runnable);

        verify(builder).delay(150L, TimeUnit.MILLISECONDS);
        verify(builder, never()).repeat(anyLong(), any(TimeUnit.class));
        verify(builder).schedule();
    }

    @Test
    void asyncTimerConvertsDelayAndPeriod() {
        provider.asyncTimer(2, 20, runnable);

        verify(builder).delay(100L, TimeUnit.MILLISECONDS);
        verify(builder).repeat(1000L, TimeUnit.MILLISECONDS);
        verify(builder).schedule();
    }

    @Test
    void tickConversionOverflowThrows() {
        assertThrows(ArithmeticException.class, () -> provider.asyncLater(Long.MAX_VALUE, runnable));
        verify(builder, never()).schedule();
    }

    @Test
    void handleReflectsTaskStatusAndCancels() {
        HandleTask handle = new HandleTask(task);
        when(task.status()).thenReturn(TaskStatus.SCHEDULED);
        assertFalse(handle.isCancelled());

        handle.cancel();
        when(task.status()).thenReturn(TaskStatus.CANCELLED);

        verify(task).cancel();
        assertTrue(handle.isCancelled());
        assertTrue(new HandleTask(null).isCancelled());
    }

    @Test
    void cancelAllCancelsOnlyMdlibTasks() {
        ScheduledTask first = mock(ScheduledTask.class);
        ScheduledTask second = mock(ScheduledTask.class);
        ScheduledTask alreadyCancelled = mock(ScheduledTask.class);
        new HandleTask(first);
        new HandleTask(second);
        new HandleTask(alreadyCancelled).cancel();

        HandleTask.cancelAll();
        HandleTask.cancelAll();

        verify(first).cancel();
        verify(second).cancel();
        verify(alreadyCancelled, times(1)).cancel();
    }

}
