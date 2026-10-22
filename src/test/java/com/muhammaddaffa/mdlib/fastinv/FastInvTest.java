package com.muhammaddaffa.mdlib.fastinv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FastInvTest {

    private final Map<Integer, ItemStack> contents = new HashMap<>();
    private MockedStatic<Bukkit> bukkit;
    private Inventory inventory;
    private FastInvManager.InventoryListener listener;
    private final Player viewer = mock(Player.class);

    @BeforeEach
    void setUp() {
        inventory = mock(Inventory.class);
        listener = new FastInvManager.InventoryListener(mock(Plugin.class));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenAnswer(invocation -> {
                    InventoryHolder holder = invocation.getArgument(0);
                    int size = invocation.getArgument(1);
                    when(inventory.getHolder()).thenReturn(holder);
                    when(inventory.getSize()).thenReturn(size);
                    return inventory;
                });
        when(inventory.getItem(anyInt())).thenAnswer(invocation -> contents.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            contents.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(inventory).setItem(anyInt(), any());
        doAnswer(invocation -> {
            contents.remove(invocation.getArgument(0));
            return null;
        }).when(inventory).clear(anyInt());
        doAnswer(invocation -> {
            contents.clear();
            return null;
        }).when(inventory).clear();
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void inventoryEventsReachSubclassAndRegisteredHandlersInOrder() {
        List<String> calls = new ArrayList<>();
        FastInv menu = new FastInv(27, "Menu") {
            @Override
            protected void onOpen(InventoryOpenEvent event) {
                calls.add("open hook");
            }

            @Override
            protected void onClick(InventoryClickEvent event) {
                calls.add("click hook");
            }

            @Override
            protected void onDrag(InventoryDragEvent event) {
                calls.add("drag hook");
            }

            @Override
            protected void onClose(InventoryCloseEvent event) {
                calls.add("close hook");
            }
        };
        Player player = mock(Player.class);
        menu.addOpenHandler(event -> calls.add("open handler"));
        menu.addClickHandler(event -> calls.add("global click"));
        menu.addDragHandler(event -> calls.add("drag handler"));
        menu.addCloseHandler(event -> calls.add("close handler"));
        menu.setItem(4, mock(ItemStack.class), event -> calls.add("slot click"));

        menu.open(player);
        InventoryOpenEvent open = mock(InventoryOpenEvent.class);
        when(open.getInventory()).thenReturn(inventory);
        listener.onInventoryOpen(open);
        InventoryClickEvent click = click(4);
        listener.onInventoryClick(click);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getInventory()).thenReturn(inventory);
        listener.onInventoryDrag(drag);
        InventoryCloseEvent close = mock(InventoryCloseEvent.class);
        when(close.getInventory()).thenReturn(inventory);
        listener.onInventoryClose(close);

        assertEquals(List.of("open hook", "open handler", "click hook", "global click", "slot click",
                "drag hook", "drag handler", "close hook", "close handler"), calls);
        verify(player).openInventory(inventory);
        verify(click).setCancelled(true);
        verify(drag).setCancelled(true);
    }

    @Test
    void replacingRemovingAndClearingItemsRemovesTheirOldClickHandlers() {
        FastInv menu = new FastInv(9, "Menu");
        List<String> calls = new ArrayList<>();
        ItemStack replacement = mock(ItemStack.class);
        menu.setItem(0, mock(ItemStack.class), event -> calls.add("old"));
        menu.setItem(0, replacement);
        menu.setItem(1, mock(ItemStack.class), event -> calls.add("removed"));
        menu.removeItem(1);
        listener.onInventoryClick(click(0));
        listener.onInventoryClick(click(1));

        assertSame(replacement, inventory.getItem(0));
        assertNull(inventory.getItem(1));
        assertTrue(calls.isEmpty());

        menu.setItem(2, mock(ItemStack.class), event -> calls.add("cleared"));
        menu.clearItems();
        listener.onInventoryClick(click(2));

        assertTrue(contents.isEmpty());
        assertTrue(calls.isEmpty());
    }

    @Test
    void navigationButtonsReplacePageContentsAndHandlersAndClearUnusedSlots() {
        PaginatedFastInv menu = new PaginatedFastInv(18, "Pages");
        menu.setClickCooldownMillis(0);
        menu.setContentSlots(List.of(0, 1));
        ItemStack first = mock(ItemStack.class);
        ItemStack second = mock(ItemStack.class);
        ItemStack third = mock(ItemStack.class);
        ItemStack previous = mock(ItemStack.class);
        ItemStack next = mock(ItemStack.class);
        List<String> calls = new ArrayList<>();
        menu.addContent(first, event -> calls.add("first"));
        menu.addContent(second, event -> calls.add("second"));
        menu.addContent(third, event -> calls.add("third"));
        menu.previousPageItem(9, previous);
        menu.nextPageItem(17, next);
        Player player = mock(Player.class);

        menu.open(player);

        assertEquals(2, menu.lastPage());
        assertTrue(menu.isFirstPage());
        assertSame(first, inventory.getItem(0));
        assertSame(second, inventory.getItem(1));
        assertSame(next, inventory.getItem(17));
        assertNull(inventory.getItem(9));
        verify(player).openInventory(inventory);
        listener.onInventoryClick(click(0));
        listener.onInventoryClick(click(17));

        assertEquals(2, menu.currentPage());
        assertTrue(menu.isLastPage());
        assertSame(third, inventory.getItem(0));
        assertNull(inventory.getItem(1));
        assertNull(inventory.getItem(17));
        assertSame(previous, inventory.getItem(9));
        listener.onInventoryClick(click(0));
        listener.onInventoryClick(click(1));
        listener.onInventoryClick(click(9));

        assertEquals(1, menu.currentPage());
        assertSame(first, inventory.getItem(0));
        assertSame(second, inventory.getItem(1));
        listener.onInventoryClick(click(1));
        assertEquals(List.of("first", "third", "second"), calls);
    }

    @Test
    void schemeBindsDecorationHandlersAndPaginationContentSlotsAcrossRows() {
        PaginatedFastInv menu = new PaginatedFastInv(18, "Scheme");
        menu.setClickCooldownMillis(0);
        ItemStack border = mock(ItemStack.class);
        ItemStack first = mock(ItemStack.class);
        ItemStack second = mock(ItemStack.class);
        ItemStack third = mock(ItemStack.class);
        List<String> calls = new ArrayList<>();
        new InventoryScheme()
                .masks("xpxxxxxxx", "xpxxxxxxx")
                .bindItem('x', border, event -> calls.add("border"))
                .bindPagination('p')
                .apply(menu);
        menu.addContent(first, event -> calls.add("first"));
        menu.addContent(second, event -> calls.add("second"));
        menu.addContent(third, event -> calls.add("third"));

        menu.openPage(1);

        assertEquals(2, menu.lastPage());
        assertSame(border, inventory.getItem(0));
        assertSame(first, inventory.getItem(1));
        assertSame(border, inventory.getItem(9));
        assertSame(second, inventory.getItem(10));
        listener.onInventoryClick(click(9));
        listener.onInventoryClick(click(10));
        menu.openNext();

        assertSame(border, inventory.getItem(9));
        assertSame(third, inventory.getItem(1));
        assertNull(inventory.getItem(10));
        listener.onInventoryClick(click(1));
        listener.onInventoryClick(click(10));
        assertEquals(List.of("border", "second", "third"), calls);
    }

    @Test
    void cooldownOnlyStartsForItemHandlersAndRejectedClicksDoNotExtendIt() {
        AtomicLong now = new AtomicLong();
        List<String> calls = new ArrayList<>();
        FastInv menu = new FastInv(holder -> {
            when(inventory.getHolder()).thenReturn(holder);
            return inventory;
        }, now::get) {
            @Override
            protected void onClick(InventoryClickEvent event) {
                calls.add("hook");
            }
        };
        menu.addClickHandler(event -> calls.add("global"));
        menu.setItem(0, mock(ItemStack.class), event -> calls.add("first"));
        menu.setItem(1, mock(ItemStack.class), event -> calls.add("second"));
        assertEquals(250, menu.getClickCooldownMillis());

        listener.onInventoryClick(click(5));
        listener.onInventoryClick(click(0));
        now.set(TimeUnit.MILLISECONDS.toNanos(200));
        InventoryClickEvent blocked = click(1);
        listener.onInventoryClick(blocked);
        assertEquals(List.of("hook", "global", "hook", "global", "first"), calls);
        verify(blocked, atLeastOnce()).setCancelled(true);

        now.set(TimeUnit.MILLISECONDS.toNanos(249));
        listener.onInventoryClick(click(5));
        listener.onInventoryClick(click(1));
        now.set(TimeUnit.MILLISECONDS.toNanos(250));
        listener.onInventoryClick(click(1));
        assertEquals(List.of("hook", "global", "hook", "global", "first",
                "hook", "global", "hook", "global", "second"), calls);
    }

    @Test
    void viewersHaveIndependentCooldownsAndCustomDurationIsRespected() {
        AtomicLong now = new AtomicLong();
        FastInv menu = new FastInv(holder -> {
            when(inventory.getHolder()).thenReturn(holder);
            return inventory;
        }, now::get);
        menu.setClickCooldownMillis(500);
        List<String> calls = new ArrayList<>();
        menu.setItem(0, mock(ItemStack.class), event -> calls.add("action"));
        listener.onInventoryClick(click(0));
        InventoryClickEvent otherViewer = click(0);
        when(otherViewer.getWhoClicked()).thenReturn(mock(Player.class));
        listener.onInventoryClick(otherViewer);
        now.set(TimeUnit.MILLISECONDS.toNanos(499));
        listener.onInventoryClick(click(0));
        assertEquals(2, calls.size());
        now.set(TimeUnit.MILLISECONDS.toNanos(500));
        listener.onInventoryClick(click(0));
        assertEquals(3, calls.size());
    }

    @Test
    void subclassesCanConfigureOrDisableTheCooldownAndNegativeDurationsAreRejected() {
        class CustomMenu extends FastInv {
            CustomMenu() {
                super(9, "Custom");
                setClickCooldownMillis(500);
            }
        }
        FastInv menu = new CustomMenu();
        assertEquals(500, menu.getClickCooldownMillis());
        assertThrows(IllegalArgumentException.class, () -> menu.setClickCooldownMillis(-1));
        assertEquals(500, menu.getClickCooldownMillis());
        List<String> calls = new ArrayList<>();
        menu.setItem(0, mock(ItemStack.class), event -> calls.add("action"));
        listener.onInventoryClick(click(0));
        menu.setClickCooldownMillis(0);
        listener.onInventoryClick(click(0));
        listener.onInventoryClick(click(0));
        assertEquals(3, calls.size());
    }

    @Test
    void paginationButtonsUseTheSameCooldown() {
        PaginatedFastInv menu = new PaginatedFastInv(18, "Pages");
        menu.setClickCooldownMillis(60_000);
        menu.setContentSlots(List.of(0));
        menu.addContent(mock(ItemStack.class));
        menu.addContent(mock(ItemStack.class));
        menu.previousPageItem(9, mock(ItemStack.class));
        menu.nextPageItem(17, mock(ItemStack.class));
        menu.openPage(1);

        listener.onInventoryClick(click(17));
        assertEquals(2, menu.currentPage());
        listener.onInventoryClick(click(9));
        assertEquals(2, menu.currentPage());
        menu.setClickCooldownMillis(0);
        listener.onInventoryClick(click(9));
        assertEquals(1, menu.currentPage());
    }

    private InventoryClickEvent click(int slot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getClickedInventory()).thenReturn(inventory);
        when(event.getRawSlot()).thenReturn(slot);
        when(event.getWhoClicked()).thenReturn(viewer);
        return event;
    }
}
