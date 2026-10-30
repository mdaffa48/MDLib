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
import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

class UpdateCheckListener {

    private final UpdateChecker instance;
    // Kept so unregister removes exactly these handlers and never the plugin's own listeners
    private final EventHandler<PostLoginEvent> joinHandler = this::notifyOnJoin;
    private final EventHandler<UpdateCheckEvent> updateCheckHandler = this::onUpdateCheck;
    private EventManager events;

    UpdateCheckListener(UpdateChecker instance) {
        this.instance = instance;
    }

    void register(EventManager events) {
        this.events = events;
        events.register(instance.getPlugin(), PostLoginEvent.class, joinHandler);
        events.register(instance.getPlugin(), UpdateCheckEvent.class, updateCheckHandler);
    }

    void unregister() {
        events.unregister(instance.getPlugin(), joinHandler);
        events.unregister(instance.getPlugin(), updateCheckHandler);
    }

    void notifyOnJoin(PostLoginEvent postLoginEvent) {
        if (!instance.isCheckedAtLeastOnce()) return;
        if (instance.getLastCheckResult() != UpdateCheckResult.NEW_VERSION_AVAILABLE) return;
        Player player = postLoginEvent.getPlayer();
        if (instance.getNotifyPermission() != null && player.hasPermission(instance.getNotifyPermission())) {
            UpdateCheckerMessages.printCheckResultToPlayer(instance, player, false);
        }
    }

    void onUpdateCheck(UpdateCheckEvent event) {
        if (!instance.isCheckedAtLeastOnce()) return;
        if (!instance.isNotifyRequesters()) return;
        if (event.getRequesters() == null) return;
        for (CommandSource commandSource : event.getRequesters()) {
            if (commandSource instanceof Player player) {
                UpdateCheckerMessages.printCheckResultToPlayer(instance, player, true);
            } else {
                UpdateCheckerMessages.printCheckResultToConsole(instance, event);
            }

        }
    }

}
