package com.muhammaddaffa.mdlib.hooks;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy {

    private static Economy economy;

    public static void init() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return;
        }
        economy = rsp.getProvider();
    }

    public static double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    public static EconomyResponse deposit(OfflinePlayer player, double amount) {
        return economy.depositPlayer(player, amount);
    }

    public static EconomyResponse withdraw(OfflinePlayer player, double amount) {
        return economy.withdrawPlayer(player, amount);
    }

    public static void set(OfflinePlayer player, double amount) {
        // withdraw all balance
        withdraw(player, getBalance(player));
        // deposit the new amount
        deposit(player, amount);
    }

}
