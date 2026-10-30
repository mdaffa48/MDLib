package com.muhammaddaffa.mdlib.worldguards;

import com.muhammaddaffa.mdlib.utils.Executor;
import com.muhammaddaffa.mdlib.worldguards.events.RegionEnterEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionEnteredEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeaveEvent;
import com.muhammaddaffa.mdlib.worldguards.events.RegionLeftEvent;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class WgEntityTest {

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<SimpleWorldGuardAPI> worldGuard;
    private MockedStatic<Executor> executor;
    private PluginManager plugins;
    private LivingEntity entity;
    private WgEntity tracked;
    private final Location from = new Location(null, 0, 0, 0);
    private final Location to = new Location(null, 10, 0, 0);
    private final List<RegionEvent> events = new ArrayList<>();
    private final List<Runnable> delayed = new ArrayList<>();

    @BeforeEach
    void setUp() {
        plugins = mock(PluginManager.class);
        entity = mock(LivingEntity.class);
        tracked = new WgEntity(entity);
        bukkit = mockStatic(Bukkit.class);
        worldGuard = mockStatic(SimpleWorldGuardAPI.class);
        executor = mockStatic(Executor.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        doAnswer(invocation -> {
            events.add(invocation.getArgument(0));
            return null;
        }).when(plugins).callEvent(any(Event.class));
        executor.when(() -> Executor.entityLater(eq(entity), eq(1L), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    delayed.add(invocation.getArgument(2));
                    return null;
                });
    }

    @AfterEach
    void tearDown() {
        executor.close();
        worldGuard.close();
        bukkit.close();
    }

    @Test
    void cancellationOfALaterEnterLeavesTheCacheAndDelayedEventsUntouched() {
        ProtectedRegion old = mock(ProtectedRegion.class);
        ProtectedRegion first = mock(ProtectedRegion.class);
        ProtectedRegion denied = mock(ProtectedRegion.class);
        tracked.getRegions().add(old);
        query(from, old);
        query(to, first, denied);
        deny(RegionEnterEvent.class, denied, List.of(old));

        assertTrue(tracked.updateRegions(MovementWay.MOVE, to, from));

        assertEquals(List.of(old), tracked.getRegions());
        assertEvents(RegionEnterEvent.class, first, denied);
        executor.verifyNoInteractions();
    }

    @Test
    void cancellationOfALeaveDoesNotCommitAnEarlierEnter() {
        ProtectedRegion old = mock(ProtectedRegion.class);
        ProtectedRegion destination = mock(ProtectedRegion.class);
        tracked.getRegions().add(old);
        query(from, old);
        query(to, destination);
        deny(RegionLeaveEvent.class, old, List.of(old));

        assertTrue(tracked.updateRegions(MovementWay.TELEPORT, to, from));

        assertEquals(List.of(old), tracked.getRegions());
        assertEquals(2, events.size());
        assertInstanceOf(RegionEnterEvent.class, events.get(0));
        assertSame(destination, events.get(0).getRegion());
        assertInstanceOf(RegionLeaveEvent.class, events.get(1));
        assertSame(old, events.get(1).getRegion());
        executor.verifyNoInteractions();
    }

    @Test
    void emptyDestinationWithALaterLeaveDenialSchedulesNoSuccessEvents() {
        ProtectedRegion first = mock(ProtectedRegion.class);
        ProtectedRegion denied = mock(ProtectedRegion.class);
        tracked.getRegions().addAll(List.of(first, denied));
        query(from, first, denied);
        query(to);
        deny(RegionLeaveEvent.class, denied, List.of(first, denied));

        assertTrue(tracked.updateRegions(MovementWay.MOVE, to, from));

        assertEquals(List.of(first, denied), tracked.getRegions());
        assertEvents(RegionLeaveEvent.class, first, denied);
        executor.verifyNoInteractions();
    }

    @Test
    void successfulTransitionCommitsBeforeSchedulingEntityCallbacks() {
        ProtectedRegion old = mock(ProtectedRegion.class);
        ProtectedRegion shared = mock(ProtectedRegion.class);
        ProtectedRegion destination = mock(ProtectedRegion.class);
        tracked.getRegions().addAll(List.of(old, shared));
        query(from, old, shared);
        query(to, shared, destination);
        doAnswer(invocation -> {
            RegionEvent event = invocation.getArgument(0);
            events.add(event);
            if (event instanceof Cancellable) {
                assertEquals(List.of(old, shared), tracked.getRegions());
            } else {
                assertEquals(List.of(shared, destination), tracked.getRegions());
            }
            return null;
        }).when(plugins).callEvent(any(Event.class));

        assertFalse(tracked.updateRegions(MovementWay.MOVE, to, from));

        assertEquals(List.of(shared, destination), tracked.getRegions());
        assertEquals(2, events.size());
        assertInstanceOf(RegionEnterEvent.class, events.get(0));
        assertInstanceOf(RegionLeaveEvent.class, events.get(1));
        assertEquals(2, delayed.size());
        executor.verify(() -> Executor.entityLater(eq(entity), eq(1L), any(Runnable.class)), times(2));
        delayed.forEach(Runnable::run);
        assertEquals(4, events.size());
        assertInstanceOf(RegionEnteredEvent.class, events.get(2));
        assertSame(destination, events.get(2).getRegion());
        assertInstanceOf(RegionLeftEvent.class, events.get(3));
        assertSame(old, events.get(3).getRegion());
        for (RegionEvent event : events) {
            assertSame(entity, event.getEntity());
            assertEquals(MovementWay.MOVE, event.getMovementWay());
        }
    }

    @Test
    void successfulEmptyDestinationClearsCacheAndDelaysLeftEvents() {
        ProtectedRegion old = mock(ProtectedRegion.class);
        tracked.getRegions().add(old);
        query(from, old);
        query(to);

        assertFalse(tracked.updateRegions(MovementWay.TELEPORT, to, from));

        assertTrue(tracked.getRegions().isEmpty());
        assertEvents(RegionLeaveEvent.class, old);
        assertEquals(1, delayed.size());
        delayed.getFirst().run();
        assertInstanceOf(RegionLeftEvent.class, events.getLast());
        assertSame(old, events.getLast().getRegion());
    }

    @Test
    void unchangedRegionsProduceNoEventsOrTasks() {
        ProtectedRegion shared = mock(ProtectedRegion.class);
        tracked.getRegions().add(shared);
        query(from, shared);
        query(to, shared);

        assertFalse(tracked.updateRegions(MovementWay.MOVE, to, from));

        assertEquals(List.of(shared), tracked.getRegions());
        assertTrue(events.isEmpty());
        executor.verifyNoInteractions();
    }

    @Test
    void spawnAtTheSameLocationInitializesCacheAndRemainsNonCancellable() {
        ProtectedRegion destination = mock(ProtectedRegion.class);
        query(to, destination);
        deny(RegionEnterEvent.class, destination, List.of());

        assertFalse(tracked.updateRegions(MovementWay.SPAWN, to, to));

        assertEquals(List.of(destination), tracked.getRegions());
        assertEvents(RegionEnterEvent.class, destination);
        assertFalse(((RegionEnterEvent) events.getFirst()).isCancellable());
        assertEquals(1, delayed.size());
        delayed.getFirst().run();
        assertInstanceOf(RegionEnteredEvent.class, events.getLast());
        assertEquals(MovementWay.SPAWN, events.getLast().getMovementWay());
    }

    private void query(Location location, ProtectedRegion... regions) {
        ApplicableRegionSet result = mock(ApplicableRegionSet.class);
        Set<ProtectedRegion> ordered = new LinkedHashSet<>(Arrays.asList(regions));
        when(result.getRegions()).thenReturn(ordered);
        when(result.iterator()).thenAnswer(invocation -> ordered.iterator());
        worldGuard.when(() -> SimpleWorldGuardAPI.getRegions(location)).thenReturn(result);
    }

    private void deny(Class<? extends RegionEvent> type, ProtectedRegion denied,
                      List<ProtectedRegion> expectedCache) {
        doAnswer(invocation -> {
            RegionEvent event = invocation.getArgument(0);
            events.add(event);
            if (event instanceof Cancellable) {
                assertEquals(expectedCache, tracked.getRegions());
            }
            if (type.isInstance(event) && event.getRegion() == denied) {
                ((Cancellable) event).setCancelled(true);
            }
            return null;
        }).when(plugins).callEvent(any(Event.class));
    }

    private void assertEvents(Class<? extends RegionEvent> type, ProtectedRegion... regions) {
        assertEquals(regions.length, events.size());
        for (int i = 0; i < regions.length; i++) {
            assertInstanceOf(type, events.get(i));
            assertSame(regions[i], events.get(i).getRegion());
        }
    }
}
