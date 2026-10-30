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

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class UpdateCheckListener implements Listener {

    private final UpdateChecker instance;

    /**
     * The class is public only because BungeeCord's EventBus invokes handlers without setAccessible, so a
     * package-private listener class fails with IllegalAccessException on every event.
     */
    UpdateCheckListener(UpdateChecker instance) {
        this.instance = instance;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void notifyOnJoin(PostLoginEvent postLoginEvent) {
        if (instance.getLastCheckResult() != UpdateCheckResult.NEW_VERSION_AVAILABLE) return;
        ProxiedPlayer player = postLoginEvent.getPlayer();
        if (instance.getNotifyPermission() != null && player.hasPermission(instance.getNotifyPermission())) {
            UpdateCheckerMessages.printCheckResultToPlayer(instance, player, false);
        }
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onUpdateCheck(UpdateCheckEvent event) {
        if (!instance.isCheckedAtLeastOnce()) return;
        if (!instance.isNotifyRequesters()) return;
        if (event.getRequesters() == null) return;
        for (CommandSender commandSender : event.getRequesters()) {
            if (commandSender instanceof ProxiedPlayer) {
                UpdateCheckerMessages.printCheckResultToPlayer(instance, (ProxiedPlayer) commandSender, true);
            } else {
                UpdateCheckerMessages.printCheckResultToConsole(instance, event);
            }

        }
    }

}
