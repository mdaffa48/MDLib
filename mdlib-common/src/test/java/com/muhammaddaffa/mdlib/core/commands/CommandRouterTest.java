package com.muhammaddaffa.mdlib.core.commands;

import com.muhammaddaffa.mdlib.commands.args.CommandContext;
import com.muhammaddaffa.mdlib.core.commands.builtin.GreedyStringArgument;
import com.muhammaddaffa.mdlib.core.commands.builtin.IntArgument;
import com.muhammaddaffa.mdlib.core.commands.builtin.LiteralArgument;
import com.muhammaddaffa.mdlib.core.commands.builtin.StringArgument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CommandRouterTest {

    private final Sender sender = new Sender();
    private final List<CommandContext> runs = new ArrayList<>();
    private final CommandRouter.Handler<Sender> handler = (s, ctx) -> runs.add(ctx);

    @Test
    void rootListsOnlyExecutablePermittedSubcommandsOneWhiteCommandPerLine() {
        TestRouter command = new TestRouter();
        command.add("set").alias("s").perm("help.set").arg("amount", new IntArgument<>()).exec(handler);
        command.add("status").exec(handler);
        command.add("admin").perm("help.admin").exec(handler);
        command.add("unfinished");
        sender.permissions.add("help.set");

        command.execute(sender, "help", new String[0]);

        assertEquals(List.of("§f/help set <number>", "§f/help status"), sender.messages);
        assertTrue(runs.isEmpty());
    }

    @Test
    void unknownSubcommandsListThePermittedOnesAndTabCompletesOnlyThem() {
        TestRouter command = new TestRouter();
        command.add("set").perm("help.set").exec(handler);
        command.add("admin").perm("help.admin").exec(handler);
        sender.permissions.add("help.set");

        command.execute(sender, "help", new String[]{"unknown"});

        assertEquals(List.of("§cUnknown subcommand: §funknown", "§f/help set"), sender.messages);
        assertEquals(List.of("set"), command.tabComplete(sender, "help", new String[]{""}));
        assertTrue(runs.isEmpty());
    }

    @Test
    void rootsWithAnExplicitHandlerExecuteItInsteadOfListingCommands() {
        TestRouter command = new TestRouter();
        command.defaultPlan().exec(handler);
        command.add("set").exec(handler);

        command.execute(sender, "help", new String[0]);

        assertEquals(1, runs.size());
        assertTrue(sender.messages.isEmpty());
    }

    @Test
    void parseErrorsShowTheMatchedUsageWithTheInvokedLabel() {
        TestRouter command = new TestRouter();
        command.alias("h");
        command.add("status").exec(handler);
        command.add("set").alias("s").arg("amount", new IntArgument<>()).exec(handler);

        command.execute(sender, "h", new String[]{"s", "wrong"});
        command.execute(sender, "help", new String[]{"set"});
        command.execute(sender, "help", new String[]{"set", "5", "extra"});

        assertEquals(List.of("§cUsage: /h set <number>", "§cUsage: /help set <number>", "§cUsage: /help set <number>"),
                sender.messages);
        assertEquals(List.of("h"), command.aliases());
        assertTrue(runs.isEmpty());
    }

    @Test
    void optionalArgumentsUseBracketLabelsAndParseToNullWhenMissing() {
        TestRouter command = new TestRouter();
        command.add("set").argOptional("amount", new IntArgument<>()).exec(handler);
        command.add("give").arg("amount", OptionalArgument.optional(new IntArgument<>())).exec(handler);

        command.execute(sender, "help", new String[0]);
        command.execute(sender, "help", new String[]{"set", "wrong"});
        command.execute(sender, "help", new String[]{"give"});

        assertEquals(List.of("§f/help set [number]", "§f/help give [number]", "§cUsage: /help set [number]"), sender.messages);
        assertEquals(1, runs.size());
        assertTrue(runs.get(0).has("amount"));
        assertNull(runs.get(0).get("amount", Integer.class));
    }

    @Test
    void rootUsageIsExplicitOrBuiltFromTheRootPlan() {
        TestRouter built = new TestRouter();
        built.defaultPlan().arg("amount", new IntArgument<>()).exec(handler);
        built.execute(sender, "help", new String[]{"wrong"});
        assertEquals("/help <number>", built.usage());

        TestRouter explicit = new TestRouter("/help <amount>", null);
        explicit.defaultPlan().arg("amount", new IntArgument<>()).exec(handler);
        explicit.execute(sender, "help", new String[]{"wrong"});

        assertEquals(List.of("§cUsage: /help <number>", "§cUsage: /help <amount>"), sender.messages);
        assertEquals("/help", new TestRouter().usage());
    }

    @Test
    void validArgumentsReachTheHandlerAndGreedyArgumentsConsumeAllText() {
        TestRouter command = new TestRouter();
        command.add("set").arg("amount", new IntArgument<>()).exec(handler);
        command.add("say").arg("message", new GreedyStringArgument<>()).exec(handler);

        command.execute(sender, "help", new String[]{"SET", "5"});
        command.execute(sender, "help", new String[]{"say", "Hello", "world"});

        assertEquals(5, runs.get(0).get("amount", Integer.class));
        assertEquals("Hello world", runs.get(1).get("message", String.class));
        assertTrue(sender.messages.isEmpty());
    }

    @Test
    void deniedSubcommandsSendThePermissionMessageAndNeverSuggest() {
        TestRouter command = new TestRouter();
        List<String> asked = new ArrayList<>();
        command.add("admin").alias("a").perm("help.admin")
                .arg("target", new StringArgument<>(), (s, prefix, prev) -> {
                    asked.add(prefix);
                    return List.of("alice", "bob");
                }).exec(handler);

        command.execute(sender, "help", new String[]{"admin", "x"});
        assertEquals(List.of("§cYou don't have permission to use this command."), sender.messages);
        assertEquals(List.of(), command.tabComplete(sender, "help", new String[]{"a", ""}));
        assertTrue(asked.isEmpty());

        sender.permissions.add("help.admin");
        assertEquals(List.of("alice"), command.tabComplete(sender, "help", new String[]{"a", "AL"}));
        assertEquals(List.of("AL"), asked);
        assertTrue(runs.isEmpty());
    }

    @Test
    void tabCompletionListsThePlaceholderAndPrimaryNamesAndFiltersByPrefix() {
        TestRouter command = new TestRouter();
        command.defaultPlan().arg("first", new StringArgument<>()).exec(handler);
        command.add("status").alias("st").exec(handler);
        command.add("set").arg("mode", new LiteralArgument<>("on", "off")).exec(handler);

        assertEquals(List.of("<first>", "status", "set"), command.tabComplete(sender, "help", new String[0]));
        assertEquals(List.of("status", "set"), command.tabComplete(sender, "help", new String[]{"s"}));
        assertEquals(List.of("on", "off"), command.tabComplete(sender, "help", new String[]{"set", "o"}));
        assertEquals(List.of(), command.tabComplete(sender, "help", new String[]{"set", "on", ""}));
    }

    @Test
    void commandPermissionHidesEverySuggestion() {
        TestRouter command = new TestRouter(null, "help.use");
        command.add("status").arg("target", new StringArgument<>(), (s, p, prev) -> List.of("alice")).exec(handler);

        assertEquals(List.of(), command.tabComplete(sender, "help", new String[0]));
        assertEquals(List.of(), command.tabComplete(sender, "help", new String[]{"status", ""}));
    }

    @Test
    void handlerFailuresSendTheInternalErrorMessage() {
        TestRouter command = new TestRouter();
        command.add("boom").exec((s, ctx) -> {
            throw new IllegalStateException("expected by the test");
        });

        command.execute(sender, "help", new String[]{"boom"});

        assertEquals(List.of("§cAn internal error occurred. See console."), sender.messages);
    }

    @Test
    void partialMatchesIgnoreCaseAndKeepOrder() {
        assertEquals(List.of("Alice", "alex"), Completions.partialMatches("al", List.of("Alice", "bob", "alex", "a")));
        assertEquals(List.of("a", "b"), Completions.partialMatches("", List.of("a", "b")));
    }

    private static final class Sender {
        private final Set<String> permissions = new HashSet<>();
        private final List<String> messages = new ArrayList<>();
    }

    private static final class TestPlan extends CommandRouter.Plan<Sender, TestPlan> {
        private TestPlan(String literal) {
            super(literal);
        }
    }

    private static final class TestRouter extends CommandRouter<Sender, TestPlan> {

        private TestRouter() {
            this(null, null);
        }

        private TestRouter(String usage, String permission) {
            super("help", "", usage, permission);
        }

        @Override
        protected TestPlan newPlan(String literal) {
            return new TestPlan(literal);
        }

        @Override
        protected boolean hasPermission(Sender sender, String permission) {
            return sender.permissions.contains(permission);
        }

        @Override
        protected void sendMessage(Sender sender, String message) {
            sender.messages.add(message);
        }

        private TestPlan add(String name) {
            return sub(name);
        }

        private TestPlan defaultPlan() {
            return root();
        }
    }
}
