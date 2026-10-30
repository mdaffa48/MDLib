package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.muhammaddaffa.mdlib.commands.internal.BungeeCommandWrapper;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.*;

public final class CommandRegistry {

    private final Plugin plugin;
    private final PluginManager pluginManager;
    private final List<BungeeCommandWrapper> registeredCommands = new ArrayList<>();

    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.pluginManager = plugin.getProxy().getPluginManager();
    }

    /** Register a command, replacing this registry's commands that use the same name or alias. */
    public void register(SimpleCommandSpec spec) {
        BungeeCommandWrapper wrapper = new BungeeCommandWrapper(spec);
        unregister(spec.name(), spec.aliases());
        pluginManager.registerCommand(plugin, wrapper);
        registeredCommands.add(wrapper);
    }

    public void registerAll(Collection<? extends SimpleCommandSpec> specs) {
        specs.forEach(this::register);
    }

    /** Unregister this registry's commands whose name or alias matches, ignoring case. Other plugins' commands stay. */
    public void unregister(String name, List<String> aliases) {
        Set<String> keys = new HashSet<>();
        keys.add(name.toLowerCase(Locale.ROOT));
        for (String alias : aliases) keys.add(alias.toLowerCase(Locale.ROOT));
        registeredCommands.removeIf(command -> {
            boolean matches = keys.contains(command.getName().toLowerCase(Locale.ROOT));
            for (String alias : command.getAliases()) matches |= keys.contains(alias.toLowerCase(Locale.ROOT));
            if (matches) pluginManager.unregisterCommand(command);
            return matches;
        });
    }

    public void unregister(String name) {
        unregister(name, List.of());
    }

    public void unregisterAll() {
        registeredCommands.forEach(pluginManager::unregisterCommand);
        registeredCommands.clear();
    }
}
