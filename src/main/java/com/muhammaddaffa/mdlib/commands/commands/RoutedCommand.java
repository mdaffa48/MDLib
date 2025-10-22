package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.commands.args.*;
import org.bukkit.command.CommandSender;

import java.util.*;

public abstract class RoutedCommand implements SimpleCommandSpec {

    // -------- Shared plumbing --------
    protected record Param(String name, ArgumentType<?> type, boolean optional) {}

    @FunctionalInterface
    public interface Handler {
        boolean run(CommandSender sender, CommandContext ctx) throws Exception;
    }

    /** Unified plan (root or sub). If literal == null → it's the root plan. */
    public static final class CommandPlan {
        private final String literal; // null for root
        private final List<Param> params = new ArrayList<>();
        private String permission; // optional (root falls back to command's root perm)
        private Handler handler;

        public CommandPlan(String literal) { this.literal = literal; }

        public CommandPlan perm(String permission) { this.permission = permission; return this; }
        public String permission() { return permission; }
        public boolean isRoot() { return literal == null; }
        public boolean isDefined() { return handler != null; }

        public CommandPlan arg(String name, ArgumentType<?> type) {
            boolean opt = (type instanceof OptionalArg<?>);
            params.add(new Param(name, type, opt));
            return this;
        }
        public CommandPlan argOptional(String name, ArgumentType<?> type) {
            ArgumentType<?> wrapped = (type instanceof OptionalArg<?>) ? type : OptionalArg.of(type);
            params.add(new Param(name, wrapped, true));
            return this;
        }
        public CommandPlan exec(Handler handler) { this.handler = handler; return this; }

        private boolean handle(CommandSender sender, String[] raw, String fallbackPerm) throws Exception {
            String permToCheck = permission != null && !permission.isBlank() ? permission : (isRoot() ? fallbackPerm : null);
            if (permToCheck != null && !permToCheck.isBlank() && !sender.hasPermission(permToCheck)) {
                sender.sendMessage("§cYou don't have permission to use this command.");
                return true;
            }

            if (handler == null) {
                return true;
            }

            TokenReader tr = new TokenReader(raw);
            CommandContext ctx = new CommandContext();

            for (Param p : params) {
                if (!tr.hasNext() && !p.type.greedy()) {
                    if (p.optional) { ctx.put(p.name, null); continue; }
                    throw new ArgParseException("Missing " + p.type.id());
                }
                Object parsed = p.type.parse(sender, tr);
                ctx.put(p.name, parsed);
            }
            return handler.run(sender, ctx);
        }

        private List<String> tab(CommandSender sender, String[] raw) {
            if (params.isEmpty()) return List.of();
            int argIndex = Math.max(0, raw.length - 1);
            if (argIndex >= params.size()) return List.of();
            Param p = params.get(argIndex);
            String prefix = raw.length == 0 ? "" : raw[raw.length - 1];
            return p.type.suggestions(sender, prefix);
        }

        public String usageString(String rootLabel) {
            StringBuilder sb = new StringBuilder("/").append(rootLabel);
            if (!isRoot()) sb.append(' ').append(literal);
            for (Param p : params) {
                String id = p.type.id();
                if (p.optional) id = "[" + id.replaceAll("[\\[\\]<>]", "") + "]";
                sb.append(' ').append(id);
            }
            return sb.toString();
        }
    }

    // -------- RoutedCommand state --------
    private final String name;
    private final String description;
    private final String explicitUsage; // may be null → auto
    private final String permission;    // root command permission (also set in Bukkit wrapper)

    private final CommandPlan rootPlan = new CommandPlan(null);
    private final Map<String, CommandPlan> subs = new LinkedHashMap<>();

    protected RoutedCommand(String name, String description, String explicitUsage, String permission) {
        this.name = name;
        this.description = description != null ? description : "";
        this.explicitUsage = explicitUsage;
        this.permission = permission;
    }

    /** Configure the root command behavior (no literal). */
    protected CommandPlan root() { return rootPlan; }

    /** Add a subcommand with the given literal. */
    protected CommandPlan sub(String literal) {
        CommandPlan p = new CommandPlan(literal);
        subs.put(literal.toLowerCase(Locale.ROOT), p);
        return p;
    }

    @Override public String name() { return name; }
    @Override public String description() { return description; }
    @Override public String permission() { return permission; }

    @Override
    public String usage() {
        if (explicitUsage != null && !explicitUsage.isEmpty()) return explicitUsage;
        if (rootPlan.isDefined()) return rootPlan.usageString(name);
        if (!subs.isEmpty()) return subs.values().iterator().next().usageString(name);
        return "/" + name;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] raw) {
        try {
            if (raw.length == 0) {
                if (rootPlan.isDefined()) return rootPlan.handle(sender, raw, permission);
                return onRoot(sender);
            }

            String first = raw[0].toLowerCase(Locale.ROOT);
            CommandPlan sub = subs.get(first);

            if (sub != null) {
                String[] rest = Arrays.copyOfRange(raw, 1, raw.length);
                return sub.handle(sender, rest, null); // sub handles its own perm (no fallback)
            }

            if (rootPlan.isDefined()) return rootPlan.handle(sender, raw, permission);

            return onUnknownSub(sender, raw[0]);

        } catch (ArgParseException apx) {
            sender.sendMessage("§c" + apx.getMessage());
            sender.sendMessage("§7Usage: §f" + usage());
            return true;
        } catch (Throwable t) {
            sender.sendMessage("§cAn internal error occurred. See console.");
            t.printStackTrace();
            return true;
        }
    }

    protected boolean onRoot(CommandSender sender) {
        String list = subs.entrySet().stream()
                .filter(e -> {
                    String perm = e.getValue().permission();
                    return perm == null || perm.isBlank() || sender.hasPermission(perm);
                })
                .map(Map.Entry::getKey)
                .reduce((a,b) -> a + "§7, §f" + b).orElse("-");
        sender.sendMessage("§7Available: §f" + list);
        sender.sendMessage("§7Usage: §f" + usage());
        return true;
    }

    protected boolean onUnknownSub(CommandSender sender, String token) {
        sender.sendMessage("§cUnknown subcommand: §f" + token);
        return onRoot(sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] raw) {
        if (raw.length == 0) {
            List<String> first = new ArrayList<>(subs.entrySet().stream()
                    .filter(e -> {
                        String perm = e.getValue().permission();
                        return perm == null || perm.isBlank() || sender.hasPermission(perm);
                    })
                    .map(Map.Entry::getKey).toList());
            if (rootPlan.isDefined()) first.addAll(rootPlan.tab(sender, new String[]{}));
            return first;
        }

        if (raw.length == 1) {
            String prefix = raw[0].toLowerCase(Locale.ROOT);
            List<String> subsMatch = subs.entrySet().stream()
                    .filter(e -> {
                        String perm = e.getValue().permission();
                        return perm == null || perm.isBlank() || sender.hasPermission(perm);
                    })
                    .map(Map.Entry::getKey)
                    .filter(k -> k.startsWith(prefix))
                    .toList();

            if (subsMatch.isEmpty() && rootPlan.isDefined()) {
                return rootPlan.tab(sender, new String[]{ raw[0] });
            }
            return subsMatch;
        }

        CommandPlan sub = subs.get(raw[0].toLowerCase(Locale.ROOT));
        if (sub != null) {
            String[] rest = Arrays.copyOfRange(raw, 1, raw.length);
            return sub.tab(sender, rest);
        }

        if (rootPlan.isDefined()) return rootPlan.tab(sender, raw);

        return List.of();
    }
}
