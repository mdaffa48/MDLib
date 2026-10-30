package com.muhammaddaffa.mdlib.consumer;

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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumerShapesTest {

    enum Mode { FAST, SLOW }

    record Foo(String value) {}

    static final class FooArg implements ArgumentType<Foo> {

        @Override
        public String id() {
            return "<foo>";
        }

        @Override
        public Foo parse(CommandSender sender, TokenReader tokens) {
            String raw = tokens.next();
            if (raw == null) throw new ArgParseException("Expected " + id());
            return new Foo(raw);
        }
    }

    static final class ShopCommand extends RoutedCommand {

        final List<String> ran = new ArrayList<>();

        ShopCommand() {
            super("shop", "Shop command", null, null);
            alias("store");
            sub("x").perm("p").alias("y").arg("n", new IntArg()).argOptional("m", new StringArg())
                    .exec((sender, ctx) -> ran.add("x:" + ctx.get("n", Integer.class) + ":" + ctx.get("m", String.class)));
            configure(sub("mode"));
            sub("say")
                    .arg("foo", new FooArg(), (sender, prefix, prev) -> List.of("bar"))
                    .arg("text", new GreedyStringArg())
                    .exec((sender, ctx) -> ran.add("say:" + ctx.get("foo", Foo.class).value() + ":" + ctx.get("text", String.class)));
        }

        @Override
        protected CommandPlan sub(String literal) {
            return super.sub(literal);
        }

        private void configure(RoutedCommand.CommandPlan plan) {
            plan.arg("mode", new EnumArg<>(Mode.class))
                    .arg("time", new DurationArg())
                    .arg("choice", new LiteralArg("a", "b"), ArgSuggester.ofDynamic((sender, prefix) -> List.of("a", "b")))
                    .arg("amount", OptionalArg.of(new IntArg()),
                            ArgSuggester.ofDynamic((sender, prefix, prev) -> List.of(String.valueOf(prev.get("time", Integer.class)))))
                    .exec((sender, ctx) -> ran.add("mode:" + ctx.get("mode", Mode.class) + ":" + ctx.get("time", Integer.class)
                            + ":" + ctx.get("choice", String.class) + ":" + ctx.get("amount", Integer.class)));
        }
    }

    @Test
    void consumerCommandRunsHandlersAndReportsUsage() {
        ShopCommand command = new ShopCommand();
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission("p")).thenReturn(true);

        command.execute(sender, "shop", new String[]{"y", "5"});
        command.execute(sender, "shop", new String[]{"x", "5", "big"});
        command.execute(sender, "shop", new String[]{"mode", "fast", "2m", "B"});
        command.execute(sender, "shop", new String[]{"say", "hi", "there", "you"});

        assertEquals(List.of("x:5:null", "x:5:big", "mode:FAST:120:b:null", "say:hi:there you"), command.ran);
        assertEquals(List.of("store"), command.aliases());
        assertEquals(List.of("120"), command.tabComplete(sender, "shop", new String[]{"mode", "fast", "2m", "a", ""}));
        assertEquals(List.of("bar"), command.tabComplete(sender, "shop", new String[]{"say", ""}));

        command.execute(sender, "shop", new String[]{"x", "five"});

        ArgumentCaptor<BaseComponent> message = ArgumentCaptor.forClass(BaseComponent.class);
        verify(sender).sendMessage(message.capture());
        assertEquals("Usage: /shop x <number> [text]", message.getValue().toPlainText());
    }

}
