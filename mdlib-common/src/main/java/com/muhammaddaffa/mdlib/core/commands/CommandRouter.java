package com.muhammaddaffa.mdlib.core.commands;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.CommandContext;
import com.muhammaddaffa.mdlib.commands.args.PartialContext;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;

import java.util.*;

public abstract class CommandRouter<S, P extends CommandRouter.Plan<S, P>> implements CommandSpec<S> {

    private record Param<S>(String name, Argument<S, ?> type, boolean optional, Suggestion<S> suggester) {}

    @FunctionalInterface
    public interface Handler<S> {
        void run(S sender, CommandContext ctx) throws Exception;
    }

    public static class Plan<S, P extends Plan<S, P>> {
        private final String literal; // null for root
        private final List<String> aliases = new ArrayList<>();
        private final List<Param<S>> params = new ArrayList<>();
        private String permission; // optional (root falls back to command's root perm)
        private Handler<S> handler;

        public Plan(String literal) { this.literal = literal; }

        @SuppressWarnings("unchecked")
        private P self() { return (P) this; }

        public P perm(String permission) { this.permission = permission; return self(); }
        public String permission() { return permission; }
        public boolean isRoot() { return literal == null; }
        public boolean isDefined() { return handler != null; }

        public P alias(List<String> names) {
            if (!isRoot()) aliases.addAll(names);
            return self();
        }

        public P alias(String... names) {
            if (!isRoot()) aliases.addAll(Arrays.asList(names));
            return self();
        }
        public List<String> aliases() { return Collections.unmodifiableList(aliases); }

        public boolean matchesToken(String token) {
            if (isRoot()) return false;
            if (literal.equalsIgnoreCase(token)) return true;
            for (String a : aliases) if (a.equalsIgnoreCase(token)) return true;
            return false;
        }

        public P arg(String name, Argument<S, ?> type, Suggestion<S> suggester) {
            boolean opt = (type instanceof OptionalArgument<?, ?>);
            params.add(new Param<>(name, type, opt, suggester));
            return self();
        }

        public P argOptional(String name, Argument<S, ?> type, Suggestion<S> suggester) {
            Argument<S, ?> wrapped = (type instanceof OptionalArgument<?, ?>) ? type : OptionalArgument.optional(type);
            params.add(new Param<>(name, wrapped, true, suggester));
            return self();
        }

        public P arg(String name, Argument<S, ?> type) {
            boolean opt = (type instanceof OptionalArgument<?, ?>);
            params.add(new Param<>(name, type, opt, null));
            return self();
        }
        public P argOptional(String name, Argument<S, ?> type) {
            Argument<S, ?> wrapped = (type instanceof OptionalArgument<?, ?>) ? type : OptionalArgument.optional(type);
            params.add(new Param<>(name, wrapped, true, null));
            return self();
        }

        public P exec(Handler<S> handler) {
            this.handler = handler;
            return self();
        }

        boolean hasPermission(CommandRouter<S, P> router, S sender, String fallbackPerm) {
            String permToCheck = permission != null && !permission.isBlank()
                    ? permission
                    : (isRoot() ? fallbackPerm : null);
            return permToCheck == null || permToCheck.isBlank() || router.hasPermission(sender, permToCheck);
        }

        boolean handle(CommandRouter<S, P> router, S sender, String[] raw, String fallbackPerm) throws Exception {
            if (!hasPermission(router, sender, fallbackPerm)) {
                router.sendMessage(sender, "§cYou don't have permission to use this command.");
                return true;
            }

            if (handler == null) {
                return true;
            }

            TokenReader tr = new TokenReader(raw);
            CommandContext ctx = new CommandContext();

            for (Param<S> p : params) {
                if (!tr.hasNext() && !p.type.greedy()) {
                    if (p.optional) { ctx.put(p.name, null); continue; }
                    throw new ArgParseException("Missing " + p.type.id());
                }
                Object parsed = p.type.parse(sender, tr);
                ctx.put(p.name, parsed);
            }
            if (tr.hasNext()) {
                throw new ArgParseException("Too many arguments.");
            }
            handler.run(sender, ctx);
            return true;
        }

        List<String> tab(CommandRouter<S, P> router, S sender, String[] raw, String fallbackPerm) {
            if (!hasPermission(router, sender, fallbackPerm)) {
                return List.of();
            }
            if (params.isEmpty()) return List.of();

            int argIndex = Math.max(0, raw.length - 1);
            if (argIndex >= params.size()) return List.of();

            // 1) Parse previous params (0..argIndex-1) into a PartialContext
            PartialContext prev = new PartialContext();
            TokenReader tr = new TokenReader(raw);
            for (int i = 0; i < argIndex; i++) {
                Param<S> p = params.get(i);

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
            Param<S> current = params.get(argIndex);
            String prefix = raw.length == 0 ? "" : raw[raw.length - 1];

            // Prefer custom suggester if present; else fall back to Argument.suggestions
            List<String> candidates = current.suggester() != null
                    ? current.suggester().suggest(sender, prefix, prev)
                    : current.type().suggestions(sender, prefix);

            if (candidates == null || candidates.isEmpty()) return List.of();

            // 3) Filter like Bukkit's StringUtil for vanilla-like behavior
            return Completions.partialMatches(prefix == null ? "" : prefix, candidates);
        }

        public String usageString(String rootLabel) {
            StringBuilder sb = new StringBuilder("/").append(rootLabel);
            if (!isRoot()) sb.append(' ').append(literal);
            for (Param<S> p : params) {
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
            Param<S> p = params.get(0);
            String name = p.name(); // use param NAME, not type id, as requested
            return p.optional() ? "[" + name + "]" : "<" + name + ">";
        }

    }

    // -------- Router state --------
    private final String name;
    private final String description;
    private final String explicitUsage; // may be null → auto
    private final String permission;    // root command permission (also set in the platform wrapper)

    private final P rootPlan;
    private final List<P> subs = new ArrayList<>();

    // root-level aliases (for /cmd itself)
    private final List<String> rootAliases = new ArrayList<>();

    protected CommandRouter(String name, String description, String explicitUsage, String permission) {
        this.name = name;
        this.description = description != null ? description : "";
        this.explicitUsage = explicitUsage;
        this.permission = permission;
        this.rootPlan = newPlan(null);
    }

    /**
     * Creates the platform's plan type. Called from the constructor, so it must not read subclass fields.
     */
    protected abstract P newPlan(String literal);

    /**
     * Whether {@code sender} has {@code permission} on this platform.
     */
    protected abstract boolean hasPermission(S sender, String permission);

    /**
     * Sends a legacy section-sign formatted message to {@code sender}.
     */
    protected abstract void sendMessage(S sender, String message);

    public CommandRouter<S, P> alias(List<String> names) {
        rootAliases.addAll(names);
        return this;
    }

    public CommandRouter<S, P> alias(String... names) {
        rootAliases.addAll(Arrays.asList(names));
        return this;
    }

    @Override public List<String> aliases() { return Collections.unmodifiableList(rootAliases); }

    /** Configure the root command behavior (no literal). */
    protected P root() { return rootPlan; }

    /** Add a subcommand with the given primary literal. */
    protected P sub(String literal) {
        P p = newPlan(literal);
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
    public void execute(S sender, String label, String[] raw) {
        P activePlan = rootPlan;
        try {
            if (raw.length == 0) {
                if (rootPlan.isDefined()) {
                    rootPlan.handle(this, sender, raw, permission);
                    return;
                }
                onRoot(sender);
                return;
            }

            String first = raw[0];

            // find matching sub by primary literal or any alias
            P matched = null;
            for (P p : subs) {
                if (p.matchesToken(first)) { matched = p; break; }
            }

            if (matched != null) {
                activePlan = matched;
                String[] rest = Arrays.copyOfRange(raw, 1, raw.length);
                matched.handle(this, sender, rest, null);
                return; // sub handles its own perm
            }

            if (rootPlan.isDefined()) {
                rootPlan.handle(this, sender, raw, permission);
                return;
            }
            onUnknownSub(sender, raw[0]);
            return;

        } catch (ArgParseException apx) {
            String commandUsage = activePlan.isRoot() && explicitUsage != null && !explicitUsage.isBlank()
                    ? explicitUsage : activePlan.usageString(label);
            sendMessage(sender, "§cUsage: " + commandUsage);
            return;
        } catch (Throwable t) {
            sendMessage(sender, "§cAn internal error occurred. See console.");
            t.printStackTrace();
            return;
        }
    }

    protected boolean onRoot(S sender) {
        for (P p : subs) {
            String perm = p.permission();
            if (p.isDefined() && (perm == null || perm.isBlank() || hasPermission(sender, perm))) {
                sendMessage(sender, "§f" + p.usageString(name));
            }
        }
        return true;
    }

    protected boolean onUnknownSub(S sender, String token) {
        sendMessage(sender, "§cUnknown subcommand: §f" + token);
        return onRoot(sender);
    }

    @Override
    public List<String> tabComplete(S sender, String alias, String[] raw) {
        if (permission != null && !permission.isBlank() && !hasPermission(sender, permission)) {
            return List.of();
        }
        // FIRST TOKEN ("/cf " or "/cf a")
        if (raw.length == 1) {
            String prefix = raw[0] == null ? "" : raw[0];
            List<String> candidates = new java.util.ArrayList<>();

            // Root first-arg placeholder (e.g. "[amount]") — show as a candidate too
            if (rootPlan.isDefined() && rootPlan.hasPermission(this, sender, permission)) {
                String ph = rootPlan.firstParamPlaceholder();
                if (ph != null) candidates.add(ph);
            }

            // Primary sub names (not aliases — cleaner UX)
            for (P p : subs) {
                String perm = p.permission();
                if (perm == null || perm.isBlank() || hasPermission(sender, perm)) {
                    var labels = p.labelsForTab(); // [primary, alias1, alias2...]
                    if (!labels.isEmpty()) candidates.add(labels.get(0)); // primary only
                }
            }

            // Filter dynamically by prefix → "acc" removes "hand"
            return Completions.partialMatches(prefix, candidates);
        }

        // ZERO TOKENS (rare), just return both placeholder + subs without filtering
        if (raw.length == 0) {
            List<String> first = new java.util.ArrayList<>();
            String ph = rootPlan.isDefined() && rootPlan.hasPermission(this, sender, permission)
                    ? rootPlan.firstParamPlaceholder() : null;
            if (ph != null) first.add(ph);
            for (P p : subs) {
                String perm = p.permission();
                if (perm == null || perm.isBlank() || hasPermission(sender, perm)) {
                    var labels = p.labelsForTab();
                    if (!labels.isEmpty()) first.add(labels.get(0));
                }
            }
            return first;
        }

        // SECOND+ TOKENS — choose sub vs root
        P matched = null;
        for (P p : subs) {
            if (p.matchesToken(raw[0])) { matched = p; break; }
        }
        if (matched != null) {
            String[] rest = java.util.Arrays.copyOfRange(raw, 1, raw.length);
            return matched.tab(this, sender, rest, null);
        }

        if (rootPlan.isDefined()) return rootPlan.tab(this, sender, raw, permission);
        return List.of();
    }

}
