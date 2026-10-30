package com.muhammaddaffa.consumer;

import com.muhammaddaffa.mdlib.commands.args.ArgParseException;
import com.muhammaddaffa.mdlib.commands.args.ArgSuggester;
import com.muhammaddaffa.mdlib.commands.args.ArgumentType;
import com.muhammaddaffa.mdlib.commands.args.OptionalArg;
import com.muhammaddaffa.mdlib.commands.args.TokenReader;
import com.muhammaddaffa.mdlib.commands.args.builtin.DurationArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.EnumArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.GreedyStringArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.LiteralArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.StringArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerShapesTest {

    enum Mode { FAST, SLOW }

    record Foo(String value) {}

    static final class FooArg implements ArgumentType<Foo> {
        @Override
        public String id() {
            return "<foo>";
        }

        @Override
        public Foo parse(CommandSource sender, TokenReader tokens) {
            String raw = tokens.next();
            if (raw == null) throw new ArgParseException("Expected " + id());
            return new Foo(raw);
        }
    }

    static final class ShopCommand extends RoutedCommand {
        final List<String> ran = new ArrayList<>();

        ShopCommand() {
            super("shop", "Shop command", null, "shop.use");
            alias("store");
            sub("x").perm("p").alias("y").arg("n", new IntArg()).argOptional("m", new StringArg())
                    .exec((sender, ctx) -> ran.add("x " + ctx.get("n", Integer.class) + " " + ctx.get("m", String.class)));
            withFoo(sub("foo"));
            sub("mode").arg("mode", new EnumArg<>(Mode.class)).arg("time", new DurationArg())
                    .arg("lit", new LiteralArg("a", "b")).arg("amount", OptionalArg.of(new IntArg()))
                    .exec((sender, ctx) -> ran.add("mode " + ctx.get("mode", Mode.class) + " " + ctx.get("time", Integer.class)
                            + " " + ctx.get("lit", String.class) + " " + ctx.get("amount", Integer.class)));
            sub("say").arg("target", new StringArg(), ArgSuggester.ofDynamic((sender, prefix) -> List.of("alice", "bob")))
                    .arg("text", new GreedyStringArg())
                    .exec((sender, ctx) -> ran.add("say " + ctx.get("target", String.class) + ": " + ctx.get("text", String.class)));
            sub("tri").arg("a", new StringArg(), ArgSuggester.ofDynamic((sender, prefix, prev) -> List.of("one", "uno")))
                    .arg("b", new StringArg(), (sender, prefix, prev) -> List.of(prev.get("a", String.class) + "-two"))
                    .exec((sender, ctx) -> ran.add("tri"));
        }

        @Override
        protected CommandPlan sub(String literal) {
            return super.sub(literal);
        }

        private void withFoo(RoutedCommand.CommandPlan plan) {
            plan.arg("foo", new FooArg()).exec((sender, ctx) -> ran.add("foo " + ctx.get("foo", Foo.class).value()));
        }
    }

    private ShopCommand command;
    private CommandSource sender;

    @BeforeEach
    void setUp() {
        command = new ShopCommand();
        sender = mock(CommandSource.class);
        when(sender.hasPermission(anyString())).thenReturn(true);
    }

    private static Component legacy(String message) {
        return LegacyComponentSerializer.legacySection().deserialize(message);
    }

    @Test
    void subcommandChainRunsHandlerWithParsedArguments() {
        command.execute(sender, "shop", new String[]{"x", "5", "hello"});
        command.execute(sender, "shop", new String[]{"Y", "7"});

        assertEquals(List.of("x 5 hello", "x 7 null"), command.ran);
        verify(sender, never()).sendMessage(any(Component.class));
    }

    @Test
    void builtinAndCustomArgumentsParse() {
        command.execute(sender, "shop", new String[]{"mode", "fast", "1m", "B"});
        command.execute(sender, "shop", new String[]{"mode", "slow", "10", "a", "3"});
        command.execute(sender, "shop", new String[]{"foo", "bar"});
        command.execute(sender, "shop", new String[]{"say", "bob", "hello", "there"});

        assertEquals(List.of("mode FAST 60 b null", "mode SLOW 10 a 3", "foo bar", "say bob: hello there"), command.ran);
    }

    @Test
    void missingArgumentSendsUsageWithInvokedLabel() {
        command.execute(sender, "store", new String[]{"x"});

        verify(sender).sendMessage(legacy("§cUsage: /store x <number> [text]"));
        assertTrue(command.ran.isEmpty());
    }

    @Test
    void subPermissionDeniedSendsPermissionMessage() {
        when(sender.hasPermission("p")).thenReturn(false);

        command.execute(sender, "shop", new String[]{"x", "5"});

        verify(sender).sendMessage(legacy("§cYou don't have permission to use this command."));
        assertTrue(command.ran.isEmpty());
    }

    @Test
    void unknownSubcommandListsPermittedSubcommands() {
        when(sender.hasPermission("p")).thenReturn(false);

        command.execute(sender, "shop", new String[]{"nope"});

        InOrder order = inOrder(sender);
        order.verify(sender).sendMessage(legacy("§cUnknown subcommand: §fnope"));
        order.verify(sender).sendMessage(legacy("§f/shop foo <foo>"));
        order.verify(sender).sendMessage(legacy("§f/shop mode <mode:fast|slow> <duration> <a|b> [number]"));
        verify(sender, never()).sendMessage(legacy("§f/shop x <number> [text]"));
    }

    @Test
    void tabCompletionUsesSuggesters() {
        assertEquals(List.of("alice", "bob"), command.tabComplete(sender, "shop", new String[]{"say", ""}));
        assertEquals(List.of("uno"), command.tabComplete(sender, "shop", new String[]{"tri", "u"}));
        assertEquals(List.of("one-two"), command.tabComplete(sender, "shop", new String[]{"tri", "one", ""}));
        assertEquals(List.of("fast", "slow"), command.tabComplete(sender, "shop", new String[]{"mode", ""}));
        assertEquals(List.of("x", "foo", "mode", "say", "tri"), command.tabComplete(sender, "shop", new String[]{""}));
        assertEquals(List.of("x", "foo", "mode", "say", "tri"), command.tabComplete(sender, "shop", new String[0]));
    }

    @Test
    void rootPermissionHidesTabCompletion() {
        when(sender.hasPermission("shop.use")).thenReturn(false);

        assertEquals(List.of(), command.tabComplete(sender, "shop", new String[]{""}));
    }

    @Test
    void metadataMatchesConstructorArguments() {
        assertEquals("shop", command.name());
        assertEquals("Shop command", command.description());
        assertEquals("shop.use", command.permission());
        assertEquals(List.of("store"), command.aliases());
        assertEquals("/shop x <number> [text]", command.usage());
    }

}
