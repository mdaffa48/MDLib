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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("UnusedReturnValue")
public class UpdateChecker extends AbstractUpdateChecker<CommandSender, UpdateChecker> {

    static final String VERSION = "3.0.4";
    private static UpdateChecker instance = null;
    private static UpdateCheckListener listener;

    private final Plugin plugin;

    /**
     * Initializes an UpdateChecker instance with a custom {@link VersionSupplier}.
     *
     * @param plugin   Instance of your plugin
     * @param supplier VersionSupplier that supplies the latest version of your plugin
     */
    public UpdateChecker(@NotNull Plugin plugin, @NotNull VersionSupplier supplier) {
        super(usedVersion(plugin), supplier);
        this.plugin = plugin;
        init();
    }

    /**
     * Initializes an UpdateChecker instance.
     *
     * @param plugin            Instance of your plugin
     * @param updateCheckSource Source where to check for updates. To use a custom source, see {@link UpdateChecker#UpdateChecker(Plugin, VersionSupplier)}
     * @param parameter         Parameter for the update checker source. See {@link UpdateCheckSource} for more information
     */
    public UpdateChecker(@NotNull Plugin plugin, @NotNull UpdateCheckSource updateCheckSource, @NotNull String parameter) {
        super(usedVersion(plugin), updateCheckSource, parameter);
        this.plugin = plugin;
        init();
    }

    private static String usedVersion(Plugin plugin) {
        return Objects.requireNonNull(plugin, "Plugin cannot be null.").getDescription().getVersion();
    }

    private void init() {
        instance = this;
        if (listener == null) {
            listener = new UpdateCheckListener(this);
            plugin.getProxy().getPluginManager().registerListener(plugin, listener);
        }
    }

    /**
     * Gets the current UpdateChecker singleton if it has been created, otherwise null.
     *
     * @return UpdateChecker instance being ran, or null if no UpdateChecker was created yet.
     * @deprecated More than one instance can exist at the same time. Keep track of the instances you created yourself.
     */
    @Deprecated
    public static UpdateChecker getInstance() {
        return instance;
    }

    /**
     * Stops the checker of {@code owner} and unregisters its listener. Does nothing when the checker belongs to another plugin.
     */
    public static void shutdown(Plugin owner) {
        if (instance == null || instance.plugin != owner) {
            return;
        }
        instance.close();
        if (listener != null) {
            owner.getProxy().getPluginManager().unregisterListener(listener);
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
     * Sets the UserAgent string using a UserAgentBuilder
     *
     * @param userAgentBuilder UserAgentBuilder instance
     * @return UpdateChecker instance being ran
     */
    public UpdateChecker setUserAgent(@NotNull UserAgentBuilder userAgentBuilder) {
        return setUserAgent(userAgentBuilder.build());
    }

    /**
     * Gets the plugin that instantiated this UpdateChecker instance
     *
     * @return Plugin that instantiated this UpdateChecker instance
     */
    protected Plugin getPlugin() {
        return plugin;
    }

    @Override
    protected AsyncTaskProvider tasks() {
        return MDLib.getTaskProvider();
    }

    @Override
    protected CommandSender[] consoleRequesters() {
        return new CommandSender[]{plugin.getProxy().getConsole()};
    }

    @Override
    protected String defaultUserAgent() {
        return UserAgentBuilder.getDefaultUserAgent().build();
    }

    @Override
    protected void fireEvent(UpdateCheckSuccess success, CommandSender[] requesters) {
        plugin.getProxy().getPluginManager().callEvent(new UpdateCheckEvent(this, success).setRequesters(requesters));
    }

}
