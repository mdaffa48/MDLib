package com.muhammaddaffa.mdlib;

import com.muhammaddaffa.mdlib.commands.CommandRegistry;
import com.muhammaddaffa.mdlib.core.LogSink;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.task.provider.BungeeProvider;
import com.muhammaddaffa.mdlib.updatechecker.UpdateChecker;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.Objects;

public final class MDLib {

    private static Plugin instance;

    private static BungeeProvider tasks;
    private static CommandRegistry commands;

    /**
     * This method should be executed in Plugin#onEnable
     */
    public static void onEnable(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (instance == plugin) {
            return;
        }
        if (instance != null) {
            throw new IllegalStateException("MDLib is already initialized for another plugin.");
        }
        instance = plugin;
        LogSink.install((level, message) -> plugin.getLogger().log(level, message));
        boolean enabled = false;
        try {
            tasks = new BungeeProvider(plugin);
            commands = new CommandRegistry(plugin);
            enabled = true;
        } finally {
            if (!enabled) {
                shutdown();
            }
        }
    }

    /**
     * This method should be executed in Plugin#onDisable
     */
    public static void shutdown() {
        if (instance != null) {
            UpdateChecker.shutdown(instance);
        }
        HandleTask.cancelAll();
        if (commands != null) {
            commands.unregisterAll();
        }
        Config.shutdown();
        Common.clearTagResolvers();
        tasks = null;
        commands = null;
        instance = null;
        LogSink.uninstall();
    }

    public static CommandRegistry getCommandRegistry() {
        return commands;
    }

    /**
     * The scheduler that Executor and UpdateChecker use, or null while MDLib is not enabled.
     */
    public static BungeeProvider getTaskProvider() {
        return tasks;
    }

    public static Plugin instance() {
        return instance;
    }

}
