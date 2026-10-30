/*
 * Copyright (c) 2022 Alexander Majka (mfnalex), JEFF Media GbR
 * Website: https://www.jeff-media.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.muhammaddaffa.mdlib.updatechecker;

import com.muhammaddaffa.mdlib.MDLib;
import com.muhammaddaffa.mdlib.core.task.AsyncTaskProvider;
import com.muhammaddaffa.mdlib.core.update.AbstractUpdateChecker;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("UnusedReturnValue")
public class UpdateChecker extends AbstractUpdateChecker<CommandSource, UpdateChecker> {

    static final String VERSION = "3.0.4";
    private static UpdateChecker instance = null;
    private static UpdateCheckListener listener;

    private final Object plugin;
    // Captured at construction so a check still running after MDLib.shutdown() never reads the cleared MDLib.server()
    private final ProxyServer server = MDLib.server();

    {
        instance = this;
    }

    /**
     * Initializes an UpdateChecker instance with a custom {@link VersionSupplier}.
     *
     * @param plugin   Instance of your plugin
     * @param supplier VersionSupplier that supplies the latest version of your plugin
     */
    public UpdateChecker(@NotNull Object plugin, @NotNull VersionSupplier supplier) {
        super(pluginVersion(plugin), supplier);
        this.plugin = plugin;
        init();
    }

    /**
     * Initializes an UpdateChecker instance.
     *
     * @param plugin            Instance of your plugin
     * @param updateCheckSource Source where to check for updates. To use a custom source, see {@link UpdateChecker#UpdateChecker(Object, VersionSupplier)}
     * @param parameter         Parameter for the update checker source. See {@link UpdateCheckSource} for more information
     */
    public UpdateChecker(@NotNull Object plugin, @NotNull UpdateCheckSource updateCheckSource, @NotNull String parameter) {
        super(pluginVersion(plugin), updateCheckSource, parameter);
        this.plugin = plugin;
        init();
    }

    private static String pluginVersion(Object plugin) {
        Objects.requireNonNull(plugin, "Plugin cannot be null.");
        return MDLib.server().getPluginManager().ensurePluginContainer(plugin).getDescription().getVersion().orElse("");
    }

    private void init() {
        if (listener == null) {
            listener = new UpdateCheckListener(this);
            listener.register(server.getEventManager());
        }
    }

    /**
     * Gets the current UpdateChecker singleton if it has been created, otherwise null.
     *
     * @return UpdateChecker instance being ran, or null if no UpdateChecker was created yet.
     * @deprecated As of SpigotUpdateChecker 1.4.0, more than one instance can exist at the same time. Keep track of the instances you created yourself.
     */
    @Deprecated
    public static UpdateChecker getInstance() {
        return instance;
    }

    /**
     * Shuts down the current UpdateChecker and unregisters its listener, but only when it belongs to {@code owner}.
     */
    public static void shutdown(Object owner) {
        if (instance == null || instance.plugin != owner) {
            return;
        }
        instance.close();
        if (listener != null) {
            listener.unregister();
            listener = null;
        }
        instance = null;
    }

    /**
     * Checks whether one version is really newer than another according to the semantic versioning scheme, including letters.
     *
     * @param myVersion    One version string
     * @param otherVersion Another version string
     * @return true if the other version is indeed newer, otherwise false
     */
    public static boolean isOtherVersionNewer(String myVersion, String otherVersion) {
        return UpdateCheckerCore.isOtherVersionNewer(myVersion, otherVersion);
    }

    /**
     * Gets the plugin that this UpdateChecker is assigned to
     *
     * @return Plugin instance
     */
    protected Object getPlugin() {
        return plugin;
    }

    PluginDescription getDescription() {
        return server.getPluginManager().ensurePluginContainer(plugin).getDescription();
    }

    String getPluginName() {
        PluginDescription description = getDescription();
        return description.getName().orElse(description.getId());
    }

    /**
     * Sets the User-Agent string using a {@link UserAgentBuilder}
     *
     * @param userAgentBuilder UserAgentBuilder instance
     * @return UpdateChecker instance being ran
     */
    public UpdateChecker setUserAgent(@NotNull UserAgentBuilder userAgentBuilder) {
        return setUserAgent(userAgentBuilder.build());
    }

    @Override
    protected AsyncTaskProvider tasks() {
        return MDLib.getTaskProvider();
    }

    @Override
    protected CommandSource[] consoleRequesters() {
        return new CommandSource[]{server.getConsoleCommandSource()};
    }

    @Override
    protected String defaultUserAgent() {
        return UserAgentBuilder.getDefaultUserAgent().build();
    }

    @Override
    protected void fireEvent(UpdateCheckSuccess success, CommandSource[] requesters) {
        server.getEventManager().fire(new UpdateCheckEvent(this, success).setRequesters(requesters));
    }

}
