package com.muhammaddaffa.mdlib;

import com.muhammaddaffa.mdlib.commands.CommandRegistry;
import com.muhammaddaffa.mdlib.core.LogSink;
import com.muhammaddaffa.mdlib.task.handleTask.HandleTask;
import com.muhammaddaffa.mdlib.task.provider.VelocityProvider;
import com.muhammaddaffa.mdlib.updatechecker.UpdateChecker;
import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Config;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;

public final class MDLib {

    private static Object instance;
    private static ProxyServer server;
    private static Path dataDirectory;

    private static CommandRegistry commands;
    private static VelocityProvider taskProvider;

    /**
     * This method should be executed when the proxy fires ProxyInitializeEvent, with the plugin's injected
     * ProxyServer, Logger and @DataDirectory path.
     */
    public static void onEnable(Object plugin, ProxyServer server, Logger logger, Path dataDirectory) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(logger, "logger");
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        if (instance == plugin) {
            return;
        }
        if (instance != null) {
            throw new IllegalStateException("MDLib is already initialized for another plugin.");
        }
        instance = plugin;
        MDLib.server = server;
        MDLib.dataDirectory = dataDirectory;
        LogSink.install((level, message) -> log(logger, level, message));
        boolean enabled = false;
        try {
            taskProvider = new VelocityProvider(server, plugin);
            commands = new CommandRegistry(server, plugin);
            enabled = true;
        } finally {
            if (!enabled) {
                shutdown();
            }
        }
    }

    /**
     * This method should be executed when the proxy fires ProxyShutdownEvent
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
        commands = null;
        taskProvider = null;
        instance = null;
        server = null;
        dataDirectory = null;
        LogSink.uninstall();
    }

    private static void log(Logger logger, Level level, String message) {
        int value = level.intValue();
        if (value >= Level.SEVERE.intValue()) {
            logger.error(message);
        } else if (value >= Level.WARNING.intValue()) {
            logger.warn(message);
        } else if (value >= Level.INFO.intValue()) {
            logger.info(message);
        } else if (value >= Level.FINE.intValue()) {
            logger.debug(message);
        } else {
            logger.trace(message);
        }
    }

    public static CommandRegistry getCommandRegistry() {
        return commands;
    }

    /**
     * The scheduler MDLib created in {@link #onEnable}, used by Executor and the UpdateChecker.
     */
    public static VelocityProvider getTaskProvider() {
        return taskProvider;
    }

    public static Object instance() {
        return instance;
    }

    public static ProxyServer server() {
        return server;
    }

    public static Path dataDirectory() {
        return dataDirectory;
    }

}
