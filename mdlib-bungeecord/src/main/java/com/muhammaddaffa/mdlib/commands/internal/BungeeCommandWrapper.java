package com.muhammaddaffa.mdlib.commands.internal;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommandWrapper extends Command implements TabExecutor {
    private final SimpleCommandSpec spec;

    public BungeeCommandWrapper(SimpleCommandSpec spec) {
        super(spec.name(), spec.permission() == null || spec.permission().isBlank() ? null : spec.permission(),
                spec.aliases().toArray(new String[0]));
        this.spec = spec;
    }

    /**
     * BungeeCord does not pass the label that was typed, so usage messages always show the primary name.
     */
    @Override
    public void execute(CommandSender sender, String[] args) {
        spec.execute(sender, getName(), args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return spec.tabComplete(sender, getName(), args);
    }

    /**
     * Identity equality. BungeeCord's Command compares name, permission and aliases, so unregistering one command
     * would otherwise also remove a look-alike command of another plugin.
     */
    @Override
    public boolean equals(Object other) {
        return this == other;
    }

    @Override
    protected boolean canEqual(Object other) {
        return this == other;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
