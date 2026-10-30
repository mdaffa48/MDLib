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

import com.velocitypowered.api.command.CommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UpdateCheckEvent {

    private final UpdateChecker instance;
    private final UpdateCheckResult result;
    private final UpdateCheckSuccess success;
    private @Nullable CommandSource[] requesters = null;

    UpdateCheckEvent(UpdateChecker instance, UpdateCheckSuccess success) {
        this.instance = instance;
        this.success = success;
        if (success == UpdateCheckSuccess.FAIL && instance.getLatestVersion() == null) {
            result = UpdateCheckResult.UNKNOWN;
        } else {
            if (instance.isUsingLatestVersion()) {
                result = UpdateCheckResult.RUNNING_LATEST_VERSION;
            } else {
                result = UpdateCheckResult.NEW_VERSION_AVAILABLE;
            }
        }
    }

    /**
     * Returns the latest version string found by the UpdateChecker, or null if all previous checks have failed.
     *
     * @return Latest version string found by the UpdateChecker, or null if all previous checks have failed
     */
    public @Nullable String getLatestVersion() {
        return instance.getLatestVersion();
    }

    /**
     * Gets an array of all CommandSources who have requested this update check. Normally this will either be the ConsoleCommandSource or a player.
     *
     * @return Array of all CommandSources who have requested this update check
     */
    public @Nullable CommandSource[] getRequesters() {
        if (requesters == null || requesters.length == 0) return null;
        return requesters;
    }

    /**
     * Sets the CommandSources who requested this update check.
     *
     * @param requesters CommandSources who requested this update check
     * @return UpdateCheckEvent instance
     */
    protected UpdateCheckEvent setRequesters(@Nullable CommandSource... requesters) {
        this.requesters = requesters;
        return this;
    }

    /**
     * Gets the result, i.e. whether a new version is available or not.
     *
     * @return UpdateCheckResult of this update check
     */
    public UpdateCheckResult getResult() {
        return result;
    }

    /**
     * Checks whether the update checking attempt was successful or failed.
     *
     * @return UpdateCheckSuccess of this update check
     */
    public UpdateCheckSuccess getSuccess() {
        return success;
    }

    /**
     * Gets the version string of the currently used plugin version.
     *
     * @return Version string of the currently used plugin version
     */
    public @NotNull String getUsedVersion() {
        return instance.getUsedVersion();
    }

}
