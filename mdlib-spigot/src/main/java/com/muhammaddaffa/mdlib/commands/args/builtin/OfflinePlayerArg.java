package com.muhammaddaffa.mdlib.commands.args.builtin;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OfflinePlayerArg implements ArgumentType<OfflinePlayer> {

    @Override
    public String id() {
        return "<player>";
    }

    @Override
    public OfflinePlayer parse(CommandSender sender, TokenReader tokens) {
        String name = tokens.next();
        if (name == null) throw new ArgParseException("Expected " + id());
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String offlineName = offlinePlayer.getName();
            if (offlineName != null && offlineName.equalsIgnoreCase(name)) {
                return offlinePlayer;
            }
        }

        throw new ArgParseException("Player '" + name + "' not found");
    }

    @Override
    public List<String> suggestions(CommandSender sender, String prefix) {
        String lowerPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);

        return Arrays.stream(Bukkit.getOfflinePlayers())
                .map(OfflinePlayer::getName)
                .filter(name -> name != null)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
                .toList();
    }
}
