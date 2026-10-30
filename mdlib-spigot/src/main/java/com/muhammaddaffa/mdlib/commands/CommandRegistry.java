package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.muhammaddaffa.mdlib.commands.internal.BukkitCommandWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;

public final class CommandRegistry {

    private final Plugin plugin;
    private final CommandMap commandMap;
    private final Map<String, Command> knownCommands;
    private final Set<Command> registeredCommands = new HashSet<>();
    private final Map<String, Command> displacedCommands = new HashMap<>();

    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.commandMap = getCommandMap();
        this.knownCommands = getKnownCommands(this.commandMap);
    }

    /** Register a command, taking priority over conflicting names and aliases. */
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
        unregister(name, List.of());
    }

    public void unregisterAll() {
        for (String key : new ArrayList<>(knownCommands.keySet())) {
            if (registeredCommands.contains(knownCommands.get(key))) knownCommands.remove(key);
        }
        registeredCommands.forEach(command -> command.unregister(commandMap));
        registeredCommands.clear();
        // Preserve any newer registration that has taken over while we were enabled.
        displacedCommands.forEach((key, command) -> {
            if (!knownCommands.containsKey(key)) knownCommands.put(key, command);
        });
        displacedCommands.clear();
    }

    private static CommandMap getCommandMap() {
        try {
            return Bukkit.getCommandMap();
        } catch (NoSuchMethodError absentOnSpigot) {
            Object server = Bukkit.getServer();
            try {
                var method = server.getClass().getMethod("getCommandMap");
                method.setAccessible(true);
                return (CommandMap) method.invoke(server);
            } catch (NoSuchMethodException absentOnOlderServers) {
                return (CommandMap) readField(server, "commandMap");
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unable to access the server command map", ex);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommands(CommandMap map) {
        try {
            return map.getKnownCommands();
        } catch (NoSuchMethodError absentOnSpigot) {
            return (Map<String, Command>) readField(map, "knownCommands");
        }
    }

    private static Object readField(Object owner, String name) {
        for (Class<?> type = owner.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException inherited) {
                // CraftServer and command-map wrappers may inherit their backing fields.
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("Unable to access command field " + name, ex);
            }
        }
        throw new IllegalStateException("Unable to find command field " + name);
    }

    private void unregisterIfPresent(String name, List<String> aliases) {
        Set<String> keys = new HashSet<>();
        keys.add(name.toLowerCase(Locale.ROOT));
        for (String alias : aliases) keys.add(alias.toLowerCase(Locale.ROOT));
        String namespace = plugin.getName().toLowerCase(Locale.ROOT) + ":";
        Set<Command> removed = new HashSet<>();
        for (Command command : registeredCommands) {
            if (keys.contains(command.getName().toLowerCase(Locale.ROOT))) removed.add(command);
        }
        for (String key : keys) {
            Command command = knownCommands.get(key);
            if (registeredCommands.contains(command)) removed.add(command);
            command = knownCommands.get(namespace + key);
            if (registeredCommands.contains(command)) removed.add(command);
        }
        for (String key : new ArrayList<>(knownCommands.keySet())) {
            if (removed.contains(knownCommands.get(key))) knownCommands.remove(key);
        }
        removed.forEach(command -> command.unregister(commandMap));
        registeredCommands.removeAll(removed);
        for (String key : keys) {
            Command previous = knownCommands.remove(key);
            if (previous != null) displacedCommands.putIfAbsent(key, previous);
        }
    }
}
