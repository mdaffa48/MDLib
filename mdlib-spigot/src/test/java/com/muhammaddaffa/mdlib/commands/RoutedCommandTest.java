package com.muhammaddaffa.mdlib.commands;

import com.muhammaddaffa.mdlib.commands.args.CommandContext;
import com.muhammaddaffa.mdlib.commands.args.ArgSuggester;
import com.muhammaddaffa.mdlib.commands.args.OptionalArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.GreedyStringArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.IntArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.OnlinePlayerArg;
import com.muhammaddaffa.mdlib.commands.args.builtin.StringArg;
import com.muhammaddaffa.mdlib.commands.commands.RoutedCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RoutedCommandTest {

    private MockedStatic<Bukkit> bukkit;
    private Player player;
    private RoutedCommand.Handler handler;

    @BeforeEach
    void setUp() {
        player = mock(Player.class);
        handler = mock(RoutedCommand.Handler.class);
        PluginManager plugins = mock(PluginManager.class);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void rootListsOnlyExecutablePermittedSubcommandsOneWhiteCommandPerLine() {
        TestCommand command = new TestCommand();
        command.add("set").alias("s").perm("help.set").arg("amount", new IntArg()).exec(handler);
        command.add("status").exec(handler);
        command.add("admin").perm("help.admin").exec(handler);
        command.add("unfinished");
        when(player.hasPermission("help.set")).thenReturn(true);

        command.execute(player, "help", new String[0]);

        var ordered = inOrder(player);
        ordered.verify(player).sendMessage("§f/help set <number>");
        ordered.verify(player).sendMessage("§f/help status");
        verify(player, times(2)).sendMessage(anyString());
        verifyNoInteractions(handler);
    }

    @Test
    void noSubcommandsAreSentWhenEverySubcommandPermissionIsDenied() {
        TestCommand command = new TestCommand();
        command.add("set").perm("help.set").exec(handler);
        command.add("admin").perm("help.admin").exec(handler);

        command.execute(player, "help", new String[0]);

        verify(player, never()).sendMessage(anyString());
        verifyNoInteractions(handler);
    }

    @Test
    void unknownSubcommandFallbackAlsoHidesCommandsWithoutPermission() {
        TestCommand command = new TestCommand();
        command.add("set").perm("help.set").exec(handler);
        command.add("admin").perm("help.admin").exec(handler);
        when(player.hasPermission("help.set")).thenReturn(true);

        command.execute(player, "help", new String[]{"unknown"});

        verify(player).sendMessage("§cUnknown subcommand: §funknown");
        verify(player).sendMessage("§f/help set");
        verify(player, times(2)).sendMessage(anyString());
        assertEquals(java.util.List.of("set"), command.tabComplete(player, "help", new String[]{""}));
        verifyNoInteractions(handler);
    }

    @Test
    void permissionIsCheckedAgainEachTimeTheCommandListIsRequested() {
        TestCommand command = new TestCommand();
        command.add("set").perm("help.set").exec(handler);
        when(player.hasPermission("help.set")).thenReturn(true, false);

        command.execute(player, "help", new String[0]);
        command.execute(player, "help", new String[0]);

        verify(player, times(1)).sendMessage("§f/help set");
        verify(player, times(1)).sendMessage(anyString());
        verifyNoInteractions(handler);
    }

    @Test
    void rootsWithAnExplicitHandlerExecuteItInsteadOfListingCommands() throws Exception {
        TestCommand command = new TestCommand();
        command.defaultPlan().exec(handler);
        command.add("set").exec(handler);

        command.execute(player, "help", new String[0]);

        verify(handler).run(eq(player), any(CommandContext.class));
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void badIntegersShowOnlyTheMatchedSubcommandUsageInRed() {
        TestCommand command = new TestCommand();
        command.add("status").exec(handler);
        command.add("set").arg("amount", new IntArg()).exec(handler);

        command.execute(player, "help", new String[]{"set", "text"});

        verify(player).sendMessage("§cUsage: /help set <number>");
        verify(player, times(1)).sendMessage(anyString());
        verifyNoInteractions(handler);
    }

    @Test
    void missingPlayerArgumentsShowTheMatchedUsage() {
        TestCommand command = new TestCommand();
        command.add("status").exec(handler);
        command.add("set").arg("target", new OnlinePlayerArg()).exec(handler);

        command.execute(player, "help", new String[]{"set"});

        verify(player).sendMessage("§cUsage: /help set <player>");
        verifyNoInteractions(handler);
    }

    @Test
    void aliasesUseTheInvokedRootLabelAndTheMatchedSubcommand() {
        TestCommand command = new TestCommand();
        command.alias("h");
        command.add("set").alias("s").arg("amount", new IntArg()).exec(handler);

        command.execute(player, "h", new String[]{"s", "wrong"});

        verify(player).sendMessage("§cUsage: /h set <number>");
        verifyNoInteractions(handler);
    }

    @Test
    void optionalIntegersUseFriendlyNumberLabelsInListsAndErrors() {
        TestCommand command = new TestCommand();
        command.add("set").arg("amount", OptionalArg.of(new IntArg())).exec(handler);

        command.execute(player, "help", new String[0]);
        command.execute(player, "help", new String[]{"set", "wrong"});

        verify(player).sendMessage("§f/help set [number]");
        verify(player).sendMessage("§cUsage: /help set [number]");
        verifyNoInteractions(handler);
    }

    @Test
    void invalidRootArgumentsUseTheRootArgumentUsage() {
        TestCommand command = new TestCommand();
        command.defaultPlan().arg("amount", new IntArg()).exec(handler);

        command.execute(player, "help", new String[]{"wrong"});

        verify(player).sendMessage("§cUsage: /help <number>");
        assertEquals("/help <number>", command.usage());
        verifyNoInteractions(handler);
    }

    @Test
    void explicitRootUsageRemainsSupported() {
        TestCommand command = new TestCommand("/help <amount>");
        command.defaultPlan().arg("amount", new IntArg()).exec(handler);

        command.execute(player, "help", new String[]{"wrong"});

        verify(player).sendMessage("§cUsage: /help <amount>");
    }

    @Test
    void extraArgumentsAreRejectedBeforeTheHandlerRuns() {
        TestCommand command = new TestCommand();
        command.add("set").arg("amount", new IntArg()).exec(handler);

        command.execute(player, "help", new String[]{"set", "5", "extra"});

        verify(player).sendMessage("§cUsage: /help set <number>");
        verifyNoInteractions(handler);
    }

    @Test
    void validArgumentsStillReachTheHandlerAndGreedyArgumentsConsumeAllText() throws Exception {
        TestCommand command = new TestCommand();
        command.add("set").arg("amount", new IntArg()).exec(handler);
        command.add("say").arg("message", new GreedyStringArg()).exec(handler);

        command.execute(player, "help", new String[]{"set", "5"});
        command.execute(player, "help", new String[]{"say", "Hello", "world"});

        ArgumentCaptor<CommandContext> context = ArgumentCaptor.forClass(CommandContext.class);
        verify(handler, times(2)).run(eq(player), context.capture());
        assertEquals(5, context.getAllValues().get(0).get("amount", Integer.class));
        assertEquals("Hello world", context.getAllValues().get(1).get("message", String.class));
        verify(player, never()).sendMessage(anyString());
    }

    @Test
    void deniedSubcommandsKeepThePermissionMessageAndDoNotRun() {
        TestCommand command = new TestCommand();
        command.add("set").perm("help.set").arg("amount", new IntArg()).exec(handler);

        command.execute(player, "help", new String[]{"set", "5"});

        verify(player).sendMessage("§cYou don't have permission to use this command.");
        verifyNoInteractions(handler);
    }

    @Test
    void deniedSubcommandsAndAliasesNeverInvokeTheirSuggester() {
        TestCommand command = new TestCommand();
        ArgSuggester suggester = mock(ArgSuggester.class);
        command.add("admin").alias("a").perm("help.admin")
                .arg("target", new StringArg(), suggester).exec(handler);

        assertEquals(List.of(), command.tabComplete(player, "help", new String[0]));
        assertEquals(List.of(), command.tabComplete(player, "help", new String[]{""}));
        assertEquals(List.of(), command.tabComplete(player, "help", new String[]{"admin", ""}));
        assertEquals(List.of(), command.tabComplete(player, "help", new String[]{"a", ""}));
        verifyNoInteractions(suggester);

        when(player.hasPermission("help.admin")).thenReturn(true);
        when(suggester.suggest(eq(player), eq("al"), any())).thenReturn(List.of("alice", "bob"));
        assertEquals(List.of("alice"), command.tabComplete(player, "help", new String[]{"a", "al"}));
    }

    @Test
    void explicitRootPlanPermissionHidesPlaceholderAndPreventsSuggestions() {
        TestCommand command = new TestCommand();
        ArgSuggester suggester = mock(ArgSuggester.class);
        command.defaultPlan().perm("help.root").arg("first", new StringArg())
                .arg("target", new StringArg(), suggester).exec(handler);
        command.add("status").exec(handler);

        assertEquals(List.of("status"), command.tabComplete(player, "help", new String[0]));
        assertEquals(List.of("status"), command.tabComplete(player, "help", new String[]{""}));
        assertEquals(List.of(), command.tabComplete(player, "help", new String[]{"value", ""}));
        verifyNoInteractions(suggester);

        when(player.hasPermission("help.root")).thenReturn(true);
        when(suggester.suggest(eq(player), eq(""), any())).thenReturn(List.of("alice"));
        assertEquals(List.of("<first>", "status"), command.tabComplete(player, "help", new String[0]));
        assertEquals(List.of("alice"), command.tabComplete(player, "help", new String[]{"value", ""}));
    }

    @Test
    void commandPermissionAlsoPreventsPublicSubcommandSuggestions() {
        TestCommand command = new TestCommand(null, "help.use");
        ArgSuggester suggester = mock(ArgSuggester.class);
        command.add("status").arg("target", new StringArg(), suggester).exec(handler);

        assertEquals(List.of(), command.tabComplete(player, "help", new String[0]));
        assertEquals(List.of(), command.tabComplete(player, "help", new String[]{""}));
        assertEquals(List.of(), command.tabComplete(player, "help", new String[]{"status", ""}));
        verifyNoInteractions(suggester);
    }

    private static final class TestCommand extends RoutedCommand {

        TestCommand() {
            super("help");
        }

        TestCommand(String usage) {
            this(usage, null);
        }

        TestCommand(String usage, String permission) {
            super("help", "", usage, permission);
        }

        CommandPlan add(String name) {
            return sub(name);
        }

        CommandPlan defaultPlan() {
            return root();
        }
    }
}
