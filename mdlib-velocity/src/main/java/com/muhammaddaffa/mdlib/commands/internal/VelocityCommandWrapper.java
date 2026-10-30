package com.muhammaddaffa.mdlib.commands.internal;

import com.muhammaddaffa.mdlib.commands.commands.SimpleCommandSpec;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;

public class VelocityCommandWrapper implements SimpleCommand {
    private final SimpleCommandSpec spec;

    public VelocityCommandWrapper(SimpleCommandSpec spec) {
        this.spec = spec;
    }

    @Override
    public void execute(Invocation invocation) {
        spec.execute(invocation.source(), invocation.alias(), invocation.arguments());
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return spec.tabComplete(invocation.source(), invocation.alias(), invocation.arguments());
    }

    /**
     * Checks the command's root permission. When this returns false, Velocity hides the command from the source
     * and passes the typed command on to the backend server instead of sending a permission message.
     */
    @Override
    public boolean hasPermission(Invocation invocation) {
        String perm = spec.permission();
        return perm == null || perm.isBlank() || invocation.source().hasPermission(perm);
    }
}
