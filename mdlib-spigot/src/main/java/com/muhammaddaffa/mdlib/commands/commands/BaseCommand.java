package com.muhammaddaffa.mdlib.commands.commands;

public abstract class BaseCommand implements SimpleCommandSpec {
    private final String name;
    private final String description;
    private final String usage;
    private final String permission;

    public BaseCommand(String name) {
        this(name, "", "/" + name, null);
    }

    public BaseCommand(String name, String permission) {
        this(name, "", "/" + name, permission);
    }

    public BaseCommand(String name, String description, String usage, String permission) {
        this.name = name;
        this.description = description != null ? description : "";
        this.usage = usage != null ? usage : ("/" + name);
        this.permission = permission;
    }

    @Override public String name() { return name; }
    @Override public String description() { return description; }
    @Override public String usage() { return usage; }
    @Override public String permission() { return permission; }
}
