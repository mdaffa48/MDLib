package com.muhammaddaffa.mdlib.hooks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VaultEconomyTest {

    private MockedStatic<Bukkit> bukkit;
    private Economy economy;
    private ServicesManager services;
    private PluginManager plugins;
    private OfflinePlayer player;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        economy = mock(Economy.class);
        services = mock(ServicesManager.class);
        plugins = mock(PluginManager.class);
        player = mock(OfflinePlayer.class);
        plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        when(server.getServicesManager()).thenReturn(services);
        when(plugins.isPluginEnabled("Vault")).thenReturn(true);
        when(economy.isEnabled()).thenReturn(true);
        when(services.getRegistration(Economy.class)).thenReturn(
                new RegisteredServiceProvider<>(Economy.class, economy, ServicePriority.Normal, plugin));
        when(economy.getBalance(player)).thenReturn(100.0);
    }

    @AfterEach
    void tearDown() {
        VaultEconomy.shutdown();
        bukkit.close();
    }

    @Test
    void unavailableAndStoppedProvidersNeverReceiveTransactionsOrBalanceReads() {
        VaultEconomy.init();
        when(economy.isEnabled()).thenReturn(false);

        assertFalse(VaultEconomy.isAvailable());
        assertTrue(Double.isNaN(VaultEconomy.getBalance(player)));
        assertFalse(VaultEconomy.deposit(player, 10).transactionSuccess());
        assertFalse(VaultEconomy.withdraw(player, 10).transactionSuccess());
        assertThrows(IllegalStateException.class, () -> VaultEconomy.set(player, 10));
        verify(economy, never()).getBalance(player);
        verify(economy, never()).depositPlayer(any(OfflinePlayer.class), anyDouble());
        verify(economy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
    }

    @Test
    void missingVaultOrRemovedServiceClearsTheCachedProvider() {
        assertTrue(VaultEconomy.isAvailable());
        when(services.getRegistration(Economy.class)).thenReturn(null);
        assertFalse(VaultEconomy.isAvailable());
        assertTrue(Double.isNaN(VaultEconomy.getBalance(player)));
        when(plugins.isPluginEnabled("Vault")).thenReturn(false);
        clearInvocations(services);
        assertFalse(VaultEconomy.withdraw(player, 10).transactionSuccess());
        verifyNoInteractions(services);
    }

    @Test
    void replacementProvidersArePickedUpWithoutRestartingTheLibrary() {
        VaultEconomy.init();
        Economy replacement = mock(Economy.class);
        when(replacement.isEnabled()).thenReturn(true);
        when(replacement.getBalance(player)).thenReturn(25.0);
        when(services.getRegistration(Economy.class)).thenReturn(
                new RegisteredServiceProvider<>(Economy.class, replacement, ServicePriority.Normal, plugin));

        assertEquals(25.0, VaultEconomy.getBalance(player));
        verify(economy, never()).getBalance(player);
    }

    @Test
    void invalidAmountsNeverReachTheProvider() {
        for (double amount : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertFalse(VaultEconomy.deposit(player, amount).transactionSuccess());
            assertFalse(VaultEconomy.withdraw(player, amount).transactionSuccess());
            assertThrows(IllegalArgumentException.class, () -> VaultEconomy.set(player, amount));
        }
        verifyNoInteractions(economy);
    }

    @Test
    void settingBalanceUsesOneTransactionForOnlyTheDifference() {
        when(economy.depositPlayer(player, 50.0)).thenReturn(success(50, 150));
        when(economy.withdrawPlayer(player, 60.0)).thenReturn(success(60, 40));

        VaultEconomy.set(player, 150);
        VaultEconomy.set(player, 40);
        VaultEconomy.set(player, 100);

        verify(economy).depositPlayer(player, 50.0);
        verify(economy).withdrawPlayer(player, 60.0);
        verify(economy, times(1)).depositPlayer(any(OfflinePlayer.class), anyDouble());
        verify(economy, times(1)).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
    }

    @Test
    void failedBalanceChangesAreReportedWithoutASecondTransaction() {
        when(economy.depositPlayer(player, 50.0)).thenReturn(new EconomyResponse(
                0, 100, EconomyResponse.ResponseType.FAILURE, "Account is locked"));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> VaultEconomy.set(player, 150));

        assertTrue(error.getMessage().contains("Account is locked"));
        verify(economy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
    }

    @Test
    void providerResponsesAreReturnedAndInvalidBalancesAreRejected() {
        EconomyResponse success = success(5, 105);
        when(economy.depositPlayer(player, 5.0)).thenReturn(success);
        assertSame(success, VaultEconomy.deposit(player, 5));
        when(economy.getBalance(player)).thenReturn(Double.NaN);
        assertThrows(IllegalStateException.class, () -> VaultEconomy.set(player, 150));
        verify(economy, never()).withdrawPlayer(any(OfflinePlayer.class), anyDouble());
    }

    private EconomyResponse success(double amount, double balance) {
        return new EconomyResponse(amount, balance, EconomyResponse.ResponseType.SUCCESS, null);
    }
}
