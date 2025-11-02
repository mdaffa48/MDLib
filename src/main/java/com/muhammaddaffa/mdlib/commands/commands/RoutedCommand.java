package com.muhammaddaffa.mdlib.commands.commands;

import com.muhammaddaffa.mdlib.commands.args.*;
import org.bukkit.command.CommandSender;
import org.bukkit.util.StringUtil;

import java.util.*;

public abstract class RoutedCommand implements SimpleCommandSpec {

    // -------- Shared plumbing --------
    protected record Param(String name, ArgumentType<?> type, boolean optional, ArgSuggester suggester) {}

    @FunctionalInterface
    public interface Handler {
        void run(CommandSender sender, CommandContext ctx) throws Exception;
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

        public CommandPlan alias(List<String> names) {
            if (!isRoot()) aliases.addAll(names);
            return this;
        }

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

        public CommandPlan arg(String name, ArgumentType<?> type, ArgSuggester suggester) {
            boolean opt = (type instanceof OptionalArg<?>);
            params.add(new Param(name, type, opt, suggester));
            return this;
        }

        public CommandPlan argOptional(String name, ArgumentType<?> type, ArgSuggester suggester) {
            ArgumentType<?> wrapped = (type instanceof OptionalArg<?>) ? type : OptionalArg.of(type);
            params.add(new Param(name, wrapped, true, suggester));
            return this;
        }

        public CommandPlan arg(String name, ArgumentType<?> type) {
            boolean opt = (type instanceof OptionalArg<?>);
            params.add(new Param(name, type, opt, null));
            return this;
        }
        public CommandPlan argOptional(String name, ArgumentType<?> type) {
            ArgumentType<?> wrapped = (type instanceof OptionalArg<?>) ? type : OptionalArg.of(type);
            params.add(new Param(name, wrapped, true, null));
            return this;
        }

        public CommandPlan exec(Handler handler) {
            this.handler = handler;
            return this;
        }

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
            handler.run(sender, ctx);
            return true;
        }

        private List<String> tab(CommandSender sender, String[] raw) {
            if (params.isEmpty()) return List.of();

            int argIndex = Math.max(0, raw.length - 1);
            if (argIndex >= params.size()) return List.of();

            // 1) Parse previous params (0..argIndex-1) into a PartialContext
            PartialContext prev = new PartialContext();
            TokenReader tr = new TokenReader(raw);
            for (int i = 0; i < argIndex; i++) {
                Param p = params.get(i);

                // If no more tokens for a required, non-greedy param → we can’t know prev args → no suggestions.
                if (!tr.hasNext() && !p.type.greedy()) {
                    if (p.optional) { prev.put(p.name(), null); continue; }
                    return List.of();
                }

                // Greedy args before current position make later params unreachable; bail out with no suggestions.
                if (p.type.greedy()) {
                    // consume the rest (for completeness)
                    p.type.parse(sender, tr);
                    return List.of();
                }

                try {
                    Object parsed = p.type.parse(sender, tr);
                    prev.put(p.name(), parsed);
                } catch (Exception e) {
                    // previous arg not valid yet → no suggestions
                    return List.of();
                }
            }

            // 2) Current param & prefix
            Param current = params.get(argIndex);
            String prefix = raw.length == 0 ? "" : raw[raw.length - 1];

            // Prefer custom ArgSuggester if present; else fall back to ArgumentType.suggestions
            List<String> candidates = current.suggester() != null
                    ? current.suggester().suggest(sender, prefix, prev)
                    : current.type().suggestions(sender, prefix);

            if (candidates == null || candidates.isEmpty()) return List.of();

            // 3) Filter with Bukkit StringUtil for vanilla-like behavior
            List<String> out = new ArrayList<>(candidates.size());
            StringUtil.copyPartialMatches(prefix == null ? "" : prefix, candidates, out);
            return out;
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

        // utility to display the placeholder "[amount]" or "<amount>" for the FIRST param
        String firstParamPlaceholder() {
            if (params.isEmpty()) return null;
            Param p = params.get(0);
            String name = p.name(); // use param NAME, not type id, as requested
            return p.optional() ? "[" + name + "]" : "<" + name + ">";
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

    public RoutedCommand(String name, String permission) {
        this(name, "", "/" + name, permission);
    }

    protected RoutedCommand(String name, String description, String explicitUsage, String permission) {
        this.name = name;
        this.description = description != null ? description : "";
        this.explicitUsage = explicitUsage;
        this.permission = permission;
    }

    public RoutedCommand alias(List<String> names) {
        rootAliases.addAll(names);
        return this;
    }

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
    public void execute(CommandSender sender, String label, String[] raw) {
        try {
            if (raw.length == 0) {
                if (rootPlan.isDefined()) {
                    rootPlan.handle(sender, raw, permission);
                    return;
                }
                onRoot(sender);
                return;
            }

            String first = raw[0];

            // find matching sub by primary literal or any alias
            CommandPlan matched = null;
            for (CommandPlan p : subs) {
                if (p.matchesToken(first)) { matched = p; break; }
            }

            if (matched != null) {
                String[] rest = Arrays.copyOfRange(raw, 1, raw.length);
                matched.handle(sender, rest, null);
                return; // sub handles its own perm
            }

            if (rootPlan.isDefined()) {
                rootPlan.handle(sender, raw, permission);
                return;
            }
            onUnknownSub(sender, raw[0]);
            return;

        } catch (ArgParseException apx) {
            sender.sendMessage("§c" + apx.getMessage());
            sender.sendMessage("§7Usage: §f" + usage());
            return;
        } catch (Throwable t) {
            sender.sendMessage("§cAn internal error occurred. See console.");
            t.printStackTrace();
            return;
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
        // FIRST TOKEN ("/cf " or "/cf a")
        if (raw.length == 1) {
            String prefix = raw[0] == null ? "" : raw[0];
            List<String> candidates = new java.util.ArrayList<>();

            // Root first-arg placeholder (e.g. "[amount]") — show as a candidate too
            if (rootPlan.isDefined()) {
                String ph = rootPlan.firstParamPlaceholder();
                if (ph != null) candidates.add(ph);
            }

            // Primary sub names (not aliases — cleaner UX)
            for (CommandPlan p : subs) {
                String perm = p.permission();
                if (perm == null || perm.isBlank() || sender.hasPermission(perm)) {
                    var labels = p.labelsForTab(); // [primary, alias1, alias2...]
                    if (!labels.isEmpty()) candidates.add(labels.get(0)); // primary only
                }
            }

            // Filter dynamically by prefix → "acc" removes "hand"
            List<String> out = new java.util.ArrayList<>(candidates.size());
            StringUtil.copyPartialMatches(prefix, candidates, out);
            return out;
        }

        // ZERO TOKENS (rare), just return both placeholder + subs without filtering
        if (raw.length == 0) {
            List<String> first = new java.util.ArrayList<>();
            String ph = rootPlan.isDefined() ? rootPlan.firstParamPlaceholder() : null;
            if (ph != null) first.add(ph);
            for (CommandPlan p : subs) {
                String perm = p.permission();
                if (perm == null || perm.isBlank() || sender.hasPermission(perm)) {
                    var labels = p.labelsForTab();
                    if (!labels.isEmpty()) first.add(labels.get(0));
                }
            }
            return first;
        }

        // SECOND+ TOKENS — choose sub vs root
        CommandPlan matched = null;
        for (CommandPlan p : subs) {
            if (p.matchesToken(raw[0])) { matched = p; break; }
        }
        if (matched != null) {
            String[] rest = java.util.Arrays.copyOfRange(raw, 1, raw.length);
            return matched.tab(sender, rest);
        }

        if (rootPlan.isDefined()) return rootPlan.tab(sender, raw);
        return List.of();
    }

}
