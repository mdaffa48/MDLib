package com.muhammaddaffa.mdlib.commands.internal;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BukkitCommandWrapper extends Command implements PluginIdentifiableCommand {
    private final Plugin owningPlugin;
    private final SimpleCommandSpec spec;

    public BukkitCommandWrapper(Plugin plugin, SimpleCommandSpec spec) {
        super(spec.name(), spec.description(), spec.usage(), spec.aliases());
        this.owningPlugin = plugin;
        this.spec = spec;

        String perm = spec.permission();
        if (perm != null && !perm.isBlank()) setPermission(perm);
    }

    @Override public @NotNull Plugin getPlugin() { return owningPlugin; }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String @NotNull [] args) {
        if (!testPermission(sender)) return true; // root permission gate (Bukkit handles)
        return spec.execute(sender, label, args);
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args) throws IllegalArgumentException {
        return spec.tabComplete(sender, alias, args);
    }
}
