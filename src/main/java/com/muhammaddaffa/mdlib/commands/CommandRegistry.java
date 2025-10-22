package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.muhammaddaffa.mdlib.commands.internal.BukkitCommandWrapper;
import com.muhammaddaffa.mdlib.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.*;

public final class CommandRegistry {

    private final Plugin plugin;
    private final CommandMap commandMap;

    public CommandRegistry(Plugin plugin) {
        this.plugin = plugin;
        this.commandMap = getCommandMap();
    }

    public void register(SimpleCommandSpec spec) {
        BukkitCommandWrapper wrapper = new BukkitCommandWrapper(plugin, spec);
        unregisterIfPresent(spec.name(), spec.aliases());
        commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), wrapper);
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

    // ---- internals ----

    private CommandMap getCommandMap() {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            return (CommandMap) f.get(Bukkit.getServer());
        } catch (Throwable t) {
            throw new IllegalStateException("Unable to access CommandMap via reflection", t);
        }
    }

    @SuppressWarnings("unchecked")
    private void unregisterIfPresent(String name, List<String> aliases) {
        if (!(commandMap instanceof SimpleCommandMap)) return;
        try {
            Field known = SimpleCommandMap.class.getDeclaredField("knownCommands");
            known.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) known.get(commandMap);

            Set<String> keys = new HashSet<>();
            String base = name.toLowerCase(Locale.ROOT);
            String ns = plugin.getName().toLowerCase(Locale.ROOT) + ":" + base;
            keys.add(base);
            keys.add(ns);
            for (String a : aliases) {
                String al = a.toLowerCase(Locale.ROOT);
                keys.add(al);
                keys.add(plugin.getName().toLowerCase(Locale.ROOT) + ":" + al);
            }

            List<String> present = keys.stream().filter(knownCommands::containsKey).toList();
            for (String k : present) knownCommands.remove(k);

        } catch (Throwable t) {
            Logger.warning("Could not fully unregister " + name + ": " + t.getMessage());
        }
    }
}
