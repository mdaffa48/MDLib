package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.core.commands.CommandRouter;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.List;

public abstract class RoutedCommand extends CommandRouter<CommandSource, RoutedCommand.CommandPlan> implements SimpleCommandSpec {

    @FunctionalInterface
    public interface Handler extends CommandRouter.Handler<CommandSource> {
    }

    public static final class CommandPlan extends CommandRouter.Plan<CommandSource, CommandPlan> {
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
    protected boolean hasPermission(CommandSource sender, String permission) {
        return sender.hasPermission(permission);
    }

    @Override
    protected void sendMessage(CommandSource sender, String message) {
        sender.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
    }

}
