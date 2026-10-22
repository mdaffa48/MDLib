package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.muhammaddaffa.mdlib.commands.internal.BukkitCommandWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;

import java.util.*;

public final class CommandRegistry {

    private final Plugin plugin;
    private final CommandMap commandMap;
    private final Set<Command> registeredCommands = new HashSet<>();

    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.commandMap = getCommandMap();
    }

    public void register(SimpleCommandSpec spec) {
        BukkitCommandWrapper wrapper = new BukkitCommandWrapper(plugin, spec);
        unregisterIfPresent(spec.name(), spec.aliases());
        commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), wrapper);
        registeredCommands.add(wrapper);
    }

    public void registerAll(Collection<? extends SimpleCommandSpec> specs) {
        specs.forEach(this::register);
    }

    public void unregister(String name, List<String> aliases) {
        unregisterIfPresent(name, aliases);
    }

    public void unregister(String name) {
        unregister(name, new ArrayList<>());
    }

    public void unregisterAll() {
        commandMap.getKnownCommands().entrySet().removeIf(entry -> registeredCommands.contains(entry.getValue()));
        registeredCommands.forEach(command -> command.unregister(commandMap));
        registeredCommands.clear();
    }

    // ---- internals ----

    private CommandMap getCommandMap() {
        return Bukkit.getCommandMap();
    }

    private void unregisterIfPresent(String name, List<String> aliases) {
        Map<String, Command> knownCommands = commandMap.getKnownCommands();
        Set<String> keys = new HashSet<>();
        keys.add(name.toLowerCase(Locale.ROOT));
        for (String alias : aliases) {
            keys.add(alias.toLowerCase(Locale.ROOT));
        }
        String namespace = plugin.getName().toLowerCase(Locale.ROOT) + ":";
        Set<Command> removed = new HashSet<>();
        for (Command command : registeredCommands) {
            if (keys.contains(command.getName().toLowerCase(Locale.ROOT))) {
                removed.add(command);
            }
        }
        for (String key : keys) {
            Command command = knownCommands.get(key);
            if (registeredCommands.contains(command)) {
                removed.add(command);
            }
            command = knownCommands.get(namespace + key);
            if (registeredCommands.contains(command)) {
                removed.add(command);
            }
        }
        knownCommands.entrySet().removeIf(entry -> removed.contains(entry.getValue()));
        removed.forEach(command -> command.unregister(commandMap));
        registeredCommands.removeAll(removed);
    }
}
