package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.core.commands.CommandRouter;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.List;

public abstract class RoutedCommand extends CommandRouter<CommandSender, RoutedCommand.CommandPlan> implements SimpleCommandSpec {

    @FunctionalInterface
    public interface Handler extends CommandRouter.Handler<CommandSender> {
    }

    public static final class CommandPlan extends CommandRouter.Plan<CommandSender, CommandPlan> {

        public CommandPlan(String literal) { super(literal); }

    }

    public RoutedCommand(String name) { this(name, null); }

    public RoutedCommand(String name, String permission) {
        this(name, "", null, permission);
    }

    protected RoutedCommand(String name, String description, String explicitUsage, String permission) {
        super(name, description, explicitUsage, permission);
    }

    @Override
    protected CommandPlan newPlan(String literal) {
        return new CommandPlan(literal);
    }

    @Override
    public RoutedCommand alias(List<String> names) {
        super.alias(names);
        return this;
    }

    @Override
    public RoutedCommand alias(String... names) {
        super.alias(names);
        return this;
    }

    @Override
    protected boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    protected void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(TextComponent.fromLegacy(message));
    }

}
