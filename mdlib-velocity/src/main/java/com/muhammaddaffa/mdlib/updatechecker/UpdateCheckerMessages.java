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

import com.muhammaddaffa.mdlib.utils.Logger;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class UpdateCheckerMessages {

    @NotNull
    private static Component createLink(@NotNull final String text, @NotNull final String link) {
        final Component lore = Component.textOfChildren(
                Component.text("Link: ").decorate(TextDecoration.BOLD),
                Component.text(link));
        // TODO: Make color configurable
        return Component.text(text, NamedTextColor.GOLD, TextDecoration.BOLD)
                .clickEvent(ClickEvent.openUrl(link))
                .hoverEvent(HoverEvent.showText(lore));
    }

    protected static void printCheckResultToConsole(UpdateChecker instance, UpdateCheckEvent event) {

        final String pluginName = instance.getPluginName();

        if (event.getSuccess() == UpdateCheckSuccess.FAIL || event.getResult() == UpdateCheckResult.UNKNOWN) {
            Logger.warning("Could not check for updates.");
            return;
        }

        if (event.getResult() == UpdateCheckResult.RUNNING_LATEST_VERSION) {
            if (instance.isSuppressUpToDateMessage()) return;
            Logger.info(String.format("You are using the latest version of %s.", pluginName));
            return;
        }

        List<String> lines = new ArrayList<>();

        lines.add(String.format("There is a new version of %s available!", pluginName));
        lines.add(" ");
        lines.add(String.format("Your version:   %s%s", instance.isColoredConsoleOutput() ? "§c" : "", event.getUsedVersion()));
        lines.add(String.format("Latest version: %s%s", instance.isColoredConsoleOutput() ? "§a" : "", event.getLatestVersion()));

        List<String> downloadLinks = instance.getAppropriateDownloadLinks();

        if (downloadLinks.size() > 0) {
            lines.add(" ");
            lines.add("Please update to the newest version.");
            lines.add(" ");
            if (downloadLinks.size() == 1) {
                lines.add("Download:");
                lines.add("  " + downloadLinks.get(0));
            } else if (downloadLinks.size() == 2) {
                lines.add(String.format("Download (%s)", instance.getNamePaidVersion()));
                lines.add("  " + downloadLinks.get(0));
                lines.add(" ");
                lines.add(String.format("Download (%s)", instance.getNameFreeVersion()));
                lines.add("  " + downloadLinks.get(1));
            }
        }

        if(instance.getSupportLink() != null) {
            lines.add(" ");
            lines.add("Support:");
            lines.add("  " + instance.getSupportLink());
        }

        if(instance.getDonationLink() != null) {
            lines.add(" ");
            lines.add("Donate:");
            lines.add("  " + instance.getDonationLink());
        }

        printNiceBoxToConsole(lines);
    }

    protected static void printCheckResultToPlayer(UpdateChecker instance, Player player, boolean showMessageWhenLatestVersion) {
        if (instance.getLastCheckResult() == UpdateCheckResult.NEW_VERSION_AVAILABLE) {
            player.sendMessage(legacy("§7There is a new version of §6" + instance.getPluginName() + "§7 available."));
            sendLinks(instance, player);
            player.sendMessage(legacy("§8Latest version: §a" + instance.getLatestVersion() + "§8 | Your version: §c" + instance.getUsedVersion()));
            player.sendMessage(Component.empty());
        } else if (instance.getLastCheckResult() == UpdateCheckResult.UNKNOWN) {
            player.sendMessage(legacy("§6" + instance.getPluginName() + "§c could not check for updates."));
        } else {
            if (showMessageWhenLatestVersion) {
                player.sendMessage(legacy("§aYou are running the latest version of §6" + instance.getPluginName()));
            }
        }
    }

    private static Component legacy(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

    private static void printNiceBoxToConsole(List<String> lines) {
        int longestLine = 0;
        for (String line : lines) {
            longestLine = Math.max(line.length(), longestLine);
        }
        longestLine += 2;
        if (longestLine > 120) longestLine = 120;
        longestLine += 2;
        StringBuilder dash = new StringBuilder(longestLine);
        Stream.generate(() -> "*").limit(longestLine).forEach(dash::append);

        Logger.warning(dash.toString());
        for (String line : lines) {
            Logger.warning(("*" + " ") + line);
        }
        Logger.warning(dash.toString());
    }

    private static void sendLinks(UpdateChecker instance, @NotNull final Player player) {

        List<Component> links = new ArrayList<>();

        List<String> downloadLinks = instance.getAppropriateDownloadLinks();

        if (downloadLinks.size() == 2) {
            links.add(createLink(String.format("Download (%s)", instance.getNamePaidVersion()), downloadLinks.get(0)));
            links.add(createLink(String.format("Download (%s)", instance.getNameFreeVersion()), downloadLinks.get(1)));
        } else if (downloadLinks.size() == 1) {
            links.add(createLink("Download", downloadLinks.get(0)));
        }
        if (instance.getDonationLink() != null) {
            links.add(createLink("Donate", instance.getDonationLink()));
        }
        if (instance.getChangelogLink() != null) {
            links.add(createLink("Changelog", instance.getChangelogLink()));
        }
        if (instance.getSupportLink() != null) {
            links.add(createLink("Support", instance.getSupportLink()));
        }

        final Component placeholder = Component.text(" | ", NamedTextColor.GRAY);

        player.sendMessage(Component.join(JoinConfiguration.separator(placeholder), links));
    }
}
