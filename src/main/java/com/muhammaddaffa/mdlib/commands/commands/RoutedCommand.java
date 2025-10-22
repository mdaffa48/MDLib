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

    public static final class CommandPlan {
        private final String literal; // null for root
        private final List<String> aliases = new ArrayList<>();
        private final List<Param> params = new ArrayList<>();
        private String permission; // optional (root falls back to command's root perm)
        private Handler handler;

        public CommandPlan(String literal) { this.literal = literal; }

        public CommandPlan perm(String permission) { this.permission = permission; return this; }
        public String permission() { return permission; }
        public boolean isRoot() { return literal == null; }
        public boolean isDefined() { return handler != null; }

        public CommandPlan alias(String... names) {
            if (!isRoot()) aliases.addAll(Arrays.asList(names));
            return this;
        }
        public List<String> aliases() { return Collections.unmodifiableList(aliases); }

        public boolean matchesToken(String token) {
            if (isRoot()) return false;
            if (literal.equalsIgnoreCase(token)) return true;
            for (String a : aliases) if (a.equalsIgnoreCase(token)) return true;
            return false;
        }

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
            String permToCheck = permission != null && !permission.isBlank()
                    ? permission
                    : (isRoot() ? fallbackPerm : null);
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

        /** Labels used for first-token tab suggestions (primary + aliases). */
        public List<String> labelsForTab() {
            if (isRoot()) return List.of();
            List<String> out = new ArrayList<>(1 + aliases.size());
            out.add(literal);
            out.addAll(aliases);
            return out;
        }
    }

    // -------- RoutedCommand state --------
    private final String name;
    private final String description;
    private final String explicitUsage; // may be null → auto
    private final String permission;    // root command permission (also set in Bukkit wrapper)

    private final CommandPlan rootPlan = new CommandPlan(null);
    private final List<CommandPlan> subs = new ArrayList<>();

    // root-level aliases (for /cmd itself)
    private final List<String> rootAliases = new ArrayList<>();

    protected RoutedCommand(String name, String description, String explicitUsage, String permission) {
        this.name = name;
        this.description = description != null ? description : "";
        this.explicitUsage = explicitUsage;
        this.permission = permission;
    }

    /** Add root command aliases (e.g., /hello, /hi, /hey). */
    public RoutedCommand alias(String... names) {
        rootAliases.addAll(Arrays.asList(names));
        return this;
    }

    @Override public List<String> aliases() { return Collections.unmodifiableList(rootAliases); }

    /** Configure the root command behavior (no literal). */
    protected CommandPlan root() { return rootPlan; }

    /** Add a subcommand with the given primary literal. */
    protected CommandPlan sub(String literal) {
        CommandPlan p = new CommandPlan(literal);
        subs.add(p);
        return p;
    }

    @Override public String name() { return name; }
    @Override public String description() { return description; }
    @Override public String permission() { return permission; }

    @Override
    public String usage() {
        if (explicitUsage != null && !explicitUsage.isEmpty()) return explicitUsage;
        if (rootPlan.isDefined()) return rootPlan.usageString(name);
        if (!subs.isEmpty()) return subs.get(0).usageString(name);
        return "/" + name;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] raw) {
        try {
            if (raw.length == 0) {
                if (rootPlan.isDefined()) return rootPlan.handle(sender, raw, permission);
                return onRoot(sender);
            }

            String first = raw[0];

            // find matching sub by primary literal or any alias
            CommandPlan matched = null;
            for (CommandPlan p : subs) {
                if (p.matchesToken(first)) { matched = p; break; }
            }

            if (matched != null) {
                String[] rest = Arrays.copyOfRange(raw, 1, raw.length);
                return matched.handle(sender, rest, null); // sub handles its own perm
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
        // show only subs the sender can access; include aliases in the list
        List<String> visible = new ArrayList<>();
        for (CommandPlan p : subs) {
            String perm = p.permission();
            if (perm == null || perm.isBlank() || sender.hasPermission(perm)) {
                // format: "give (g, grant)" if aliases exist
                List<String> labels = p.labelsForTab();
                String primary = labels.isEmpty() ? "" : labels.get(0);
                List<String> rest = labels.size() > 1 ? labels.subList(1, labels.size()) : List.of();
                if (rest.isEmpty()) visible.add(primary);
                else visible.add(primary + " §8(" + String.join(", ", rest) + ")§7");
            }
        }
        String list = visible.isEmpty() ? "-" : String.join("§7, §f", visible);
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
            // Suggest all visible sub labels; also root arg suggestions (first param) if root defined
            List<String> first = new ArrayList<>();
            for (CommandPlan p : subs) {
                String perm = p.permission();
                if (perm == null || perm.isBlank() || sender.hasPermission(perm)) {
                    first.addAll(p.labelsForTab());
                }
            }
            if (rootPlan.isDefined()) first.addAll(rootPlan.tab(sender, new String[]{}));
            return first;
        }

        if (raw.length == 1) {
            String prefix = raw[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (CommandPlan p : subs) {
                String perm = p.permission();
                if (perm != null && !perm.isBlank() && !sender.hasPermission(perm)) continue;
                for (String label : p.labelsForTab()) {
                    if (label.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(label);
                }
            }
            if (out.isEmpty() && rootPlan.isDefined()) {
                return rootPlan.tab(sender, new String[]{ raw[0] });
            }
            return out;
        }

        // raw.length >= 2
        CommandPlan matched = null;
        for (CommandPlan p : subs) {
            if (p.matchesToken(raw[0])) { matched = p; break; }
        }
        if (matched != null) {
            String[] rest = Arrays.copyOfRange(raw, 1, raw.length);
            return matched.tab(sender, rest);
        }

        if (rootPlan.isDefined()) return rootPlan.tab(sender, raw);

        return List.of();
    }
}
