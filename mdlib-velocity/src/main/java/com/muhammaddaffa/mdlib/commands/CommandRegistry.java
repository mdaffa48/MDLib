package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.muhammaddaffa.mdlib.commands.internal.VelocityCommandWrapper;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.*;

public final class CommandRegistry {

    private final Object plugin;
    private final CommandManager commandManager;
    // Lowercased name and aliases of every command this registry registered
    private final Map<CommandMeta, Set<String>> registeredCommands = new LinkedHashMap<>();

    public CommandRegistry(ProxyServer server, Object plugin) {
        this.plugin = plugin;
        this.commandManager = server.getCommandManager();
    }

    /** Register a command, replacing this registry's own commands that share its name or an alias. */
    public void register(SimpleCommandSpec spec) {
        unregisterIfPresent(spec.name(), spec.aliases());
        CommandMeta meta = commandManager.metaBuilder(spec.name())
                .aliases(spec.aliases().toArray(new String[0]))
                .plugin(plugin)
                .build();
        commandManager.register(meta, new VelocityCommandWrapper(spec));
        registeredCommands.put(meta, keys(spec.name(), spec.aliases()));
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
        registeredCommands.keySet().forEach(commandManager::unregister);
        registeredCommands.clear();
    }

    private void unregisterIfPresent(String name, List<String> aliases) {
        Set<String> keys = keys(name, aliases);
        registeredCommands.entrySet().removeIf(entry -> {
            if (Collections.disjoint(entry.getValue(), keys)) return false;
            commandManager.unregister(entry.getKey());
            return true;
        });
    }

    private static Set<String> keys(String name, List<String> aliases) {
        Set<String> keys = new HashSet<>();
        keys.add(name.toLowerCase(Locale.ROOT));
        for (String alias : aliases) keys.add(alias.toLowerCase(Locale.ROOT));
        return keys;
    }
}
