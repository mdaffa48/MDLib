package com.muhammaddaffa.mdlib.hooks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Objects;

public class VaultEconomy {

    private static Economy economy;

    public static synchronized void init() {
        economy = null;
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null && rsp.getProvider() != null && rsp.getProvider().isEnabled()) {
            economy = rsp.getProvider();
        }
    }

    public static synchronized void shutdown() {
        economy = null;
    }

    public static boolean isAvailable() {
        return provider() != null;
    }

    private static synchronized Economy provider() {
        init();
        return economy;
    }

    public static double getBalance(OfflinePlayer player) {
        Objects.requireNonNull(player, "player");
        Economy provider = provider();
        return provider == null ? Double.NaN : provider.getBalance(player);
    }

    public static EconomyResponse deposit(OfflinePlayer player, double amount) {
        Objects.requireNonNull(player, "player");
        if (!Double.isFinite(amount) || amount < 0) {
            return failure("Amount must be finite and non-negative.");
        }
        Economy provider = provider();
        return provider == null ? failure("Vault economy is unavailable.") : provider.depositPlayer(player, amount);
    }

    public static EconomyResponse withdraw(OfflinePlayer player, double amount) {
        Objects.requireNonNull(player, "player");
        if (!Double.isFinite(amount) || amount < 0) {
            return failure("Amount must be finite and non-negative.");
        }
        Economy provider = provider();
        return provider == null ? failure("Vault economy is unavailable.") : provider.withdrawPlayer(player, amount);
    }

    public static void set(OfflinePlayer player, double amount) {
        Objects.requireNonNull(player, "player");
        if (!Double.isFinite(amount) || amount < 0) {
            throw new IllegalArgumentException("Amount must be finite and non-negative.");
        }
        Economy provider = provider();
        if (provider == null) {
            throw new IllegalStateException("Vault economy is unavailable.");
        }
        double difference = amount - provider.getBalance(player);
        if (!Double.isFinite(difference)) {
            throw new IllegalStateException("Economy returned an invalid balance.");
        }
        if (difference == 0) {
            return;
        }
        EconomyResponse response = difference > 0
                ? provider.depositPlayer(player, difference)
                : provider.withdrawPlayer(player, -difference);
        if (response == null || !response.transactionSuccess()) {
            throw new IllegalStateException("Unable to set balance: "
                    + (response == null ? "no economy response" : response.errorMessage));
        }
    }

    private static EconomyResponse failure(String message) {
        return new EconomyResponse(0, Double.NaN, EconomyResponse.ResponseType.FAILURE, message);
    }

}
