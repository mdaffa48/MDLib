package com.muhammaddaffa.mdlib.updatechecker;

import com.muhammaddaffa.mdlib.task.ExecutorManager;
import com.muhammaddaffa.mdlib.task.ExecutorProvider;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateCheckerTest {

    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ExecutorManager> executors;
    private JavaPlugin plugin;
    private PluginManager plugins;
    private ConsoleCommandSender console;
    private ExecutorProvider provider;
    private boolean onMainThread;
    private final List<String> threads = new ArrayList<>();

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        plugins = mock(PluginManager.class);
        console = mock(ConsoleCommandSender.class);
        when(plugin.getDescription()).thenReturn(new PluginDescriptionFile("UpdateTest", " 1.0 ", "example.Main"));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(plugins);
        bukkit.when(Bukkit::getConsoleSender).thenReturn(console);
        provider = mock(ExecutorProvider.class);
        when(provider.async(any())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        });
        when(provider.sync(any())).thenAnswer(invocation -> {
            onMainThread = true;
            try {
                invocation.<Runnable>getArgument(0).run();
            } finally {
                onMainThread = false;
            }
            return null;
        });
        doAnswer(invocation -> {
            threads.add("event:" + onMainThread);
            return null;
        }).when(plugins).callEvent(any());
        executors = mockStatic(ExecutorManager.class);
        executors.when(ExecutorManager::getProvider).thenReturn(provider);
    }

    @AfterEach
    void tearDown() {
        UpdateChecker.shutdown(plugin);
        executors.close();
        bukkit.close();
    }

    @Test
    void newerSuppliedVersionIsReportedToTheSuccessCallbackAndAsAnEvent() {
        List<String> found = new ArrayList<>();
        UpdateChecker checker = new UpdateChecker(plugin, () -> "1.1")
                .setUserAgent("test")
                .onSuccess((requesters, latest) -> {
                    found.add(latest);
                    threads.add("success:" + onMainThread);
                });

        assertEquals("1.0", checker.getUsedVersion());
        assertEquals(UpdateCheckResult.UNKNOWN, checker.getLastCheckResult());

        checker.checkNow();

        assertEquals(List.of("1.1"), found);
        assertEquals(List.of("success:true", "event:true"), threads);
        assertEquals("1.1", checker.getLatestVersion());
        assertEquals(UpdateCheckResult.NEW_VERSION_AVAILABLE, checker.getLastCheckResult());
        assertFalse(checker.isUsingLatestVersion());
        assertTrue(checker.isCheckedAtLeastOnce());
        verify(provider, times(1)).async(any());
        verify(provider, times(1)).sync(any());
        verify(plugins).callEvent(argThat(event -> event instanceof UpdateCheckEvent update
                && update.getSuccess() == UpdateCheckSuccess.SUCCESS
                && update.getRequesters().length == 1 && update.getRequesters()[0] == console));
    }

    @Test
    void olderOrEqualSuppliedVersionsCountAsRunningTheLatestVersion() {
        UpdateChecker older = new UpdateChecker(plugin, () -> "0.9").setUserAgent("test");
        older.checkNow();
        assertEquals("1.0", older.getLatestVersion());
        assertEquals(UpdateCheckResult.RUNNING_LATEST_VERSION, older.getLastCheckResult());
        assertTrue(older.isUsingLatestVersion());

        UpdateChecker same = new UpdateChecker(plugin, () -> "1.0").setUserAgent("test");
        same.checkNow();
        assertEquals(UpdateCheckResult.RUNNING_LATEST_VERSION, same.getLastCheckResult());
    }

    @Test
    void failedFetchGoesToTheFailCallbackAndFiresAFailEvent() {
        IOException failure = new IOException("offline");
        List<Exception> failures = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        CommandSender requester = mock(CommandSender.class);
        UpdateChecker checker = new UpdateChecker(plugin, () -> {
            throw failure;
        }).setUserAgent("test")
                .onFail((requesters, ex) -> {
                    failures.add(ex);
                    threads.add("fail:" + onMainThread);
                })
                .onSuccess((requesters, latest) -> successes.add(latest));

        checker.checkNow(requester);

        assertEquals(List.of(failure), failures);
        assertEquals(List.of("fail:true", "event:true"), threads);
        assertTrue(successes.isEmpty());
        assertNull(checker.getLatestVersion());
        verify(provider, times(1)).async(any());
        verify(provider, times(2)).sync(any());
        verify(plugins).callEvent(argThat(event -> event instanceof UpdateCheckEvent update
                && update.getSuccess() == UpdateCheckSuccess.FAIL));
    }

    @Test
    void sourceParametersAreValidated() {
        assertThrows(IllegalArgumentException.class,
                () -> new UpdateChecker(plugin, UpdateCheckSource.GITHUB_RELEASE_TAG, "only-owner"));
        assertThrows(IllegalArgumentException.class,
                () -> new UpdateChecker(plugin, UpdateCheckSource.HANGAR, "owner/project"));
        assertThrows(NullPointerException.class,
                () -> new UpdateChecker(plugin, null, "1"));
        assertDoesNotThrow(() -> new UpdateChecker(plugin, UpdateCheckSource.GITHUB_RELEASE_TAG, "owner/repo"));
    }

    @Test
    void resourceIdLinksFollowTheSource() {
        UpdateChecker spigot = new UpdateChecker(plugin, UpdateCheckSource.SPIGOT, "123")
                .setDownloadLink(123).setChangelogLink(123);
        assertEquals(List.of("https://www.spigotmc.org/resources/123"), spigot.getAppropriateDownloadLinks());
        assertEquals("https://www.spigotmc.org/resources/123/history", spigot.getChangelogLink());

        UpdateChecker polymart = new UpdateChecker(plugin, UpdateCheckSource.POLYMART, "9")
                .setFreeDownloadLink(9).setPaidDownloadLink(10).setChangelogLink(9);
        assertEquals(List.of("https://polymart.org/resource/10", "https://polymart.org/resource/9"),
                polymart.getAppropriateDownloadLinks());
        assertEquals("https://polymart.org/resource/9/updates", polymart.getChangelogLink());

        UpdateChecker custom = new UpdateChecker(plugin, UpdateCheckSource.CUSTOM_URL, "https://example.org/v")
                .setDownloadLink(5);
        assertTrue(custom.getAppropriateDownloadLinks().isEmpty());
    }

    @Test
    void versionComparisonFollowsMavenOrdering() {
        assertTrue(UpdateChecker.isOtherVersionNewer("1.0", "1.1"));
        assertTrue(UpdateChecker.isOtherVersionNewer("1.0-SNAPSHOT", "1.0"));
        assertTrue(UpdateChecker.isOtherVersionNewer("1.9", "1.10"));
        assertFalse(UpdateChecker.isOtherVersionNewer("1.1", "1.0"));
        assertFalse(UpdateChecker.isOtherVersionNewer("1.0", "1.0"));
    }
}
