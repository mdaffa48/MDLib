package com.muhammaddaffa.mdlib.core.update;

import com.muhammaddaffa.mdlib.core.task.AsyncTaskProvider;
import com.muhammaddaffa.mdlib.core.task.TaskHandle;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckResult;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckSource;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckSuccess;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckerCore;
import com.muhammaddaffa.mdlib.updatechecker.VersionSupplier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckerBaseTest {

    @Test
    void successfulChecksReportTheNewerVersionToCallbackAndEvent() {
        List<String> found = new ArrayList<>();
        TestChecker checker = new TestChecker(" 1.0 ", () -> "1.2");
        checker.onSuccess((requesters, latest) -> found.add(requesters[0] + ":" + latest));

        assertEquals(UpdateCheckResult.UNKNOWN, checker.getLastCheckResult());
        checker.checkNow();

        assertEquals("1.0", checker.getUsedVersion());
        assertEquals(List.of("console:1.2"), found);
        assertEquals(List.of(UpdateCheckSuccess.SUCCESS), checker.events);
        assertEquals(UpdateCheckResult.NEW_VERSION_AVAILABLE, checker.getLastCheckResult());
        assertTrue(checker.isCheckedAtLeastOnce());
    }

    @Test
    void olderVersionsCountAsLatestAndFailuresGoToOnFail() {
        TestChecker older = new TestChecker("2.0", () -> "1.9");
        older.checkNow("admin");
        assertEquals("2.0", older.getLatestVersion());
        assertTrue(older.isUsingLatestVersion());

        IOException offline = new IOException("offline");
        List<Exception> failures = new ArrayList<>();
        TestChecker failing = new TestChecker("1.0", () -> {
            throw offline;
        });
        failing.onFail((requesters, ex) -> failures.add(ex)).checkNow();
        assertEquals(List.of(offline), failures);
        assertEquals(List.of(UpdateCheckSuccess.FAIL), failing.events);
        assertNull(failing.getLatestVersion());
    }

    @Test
    void checksStillQueuedWhenTheCheckerClosesReportNothing() {
        List<String> found = new ArrayList<>();
        List<Exception> failures = new ArrayList<>();
        int[] fetches = {0};
        TestChecker checker = new TestChecker("1.0", () -> {
            fetches[0]++;
            return "1.2";
        });
        checker.deferAsync = true;
        checker.onSuccess((requesters, latest) -> found.add(latest)).onFail((requesters, ex) -> failures.add(ex));

        checker.checkNow();
        checker.close();
        checker.deferred.forEach(Runnable::run);

        assertEquals(0, fetches[0]);
        assertTrue(found.isEmpty());
        assertTrue(failures.isEmpty());
        assertTrue(checker.events.isEmpty());
        assertNull(checker.getLatestVersion());
    }

    @Test
    void checksClosedWhileFetchingReportNothing() {
        List<String> found = new ArrayList<>();
        TestChecker[] holder = new TestChecker[1];
        holder[0] = new TestChecker("1.0", () -> {
            holder[0].close();
            return "1.2";
        });
        holder[0].onSuccess((requesters, latest) -> found.add(latest)).checkNow();

        assertTrue(found.isEmpty());
        assertTrue(holder[0].events.isEmpty());
        assertNull(holder[0].getLatestVersion());
    }

    @Test
    void closingInsideOnSuccessDropsTheEvent() {
        TestChecker checker = new TestChecker("1.0", () -> "1.2");
        checker.onSuccess((requesters, latest) -> checker.close()).checkNow();

        assertEquals("1.2", checker.getLatestVersion());
        assertTrue(checker.events.isEmpty());
    }

    @Test
    void failuresAfterCloseAreNotReported() {
        List<Exception> failures = new ArrayList<>();
        TestChecker[] holder = new TestChecker[1];
        holder[0] = new TestChecker("1.0", () -> {
            holder[0].close();
            throw new IOException("closed while fetching");
        });
        holder[0].onFail((requesters, ex) -> failures.add(ex)).checkNow();

        assertTrue(failures.isEmpty());
        assertTrue(holder[0].events.isEmpty());
    }

    @Test
    void scheduledChecksRunEveryPeriodInTicksAndReportToTheConsole() {
        List<String> found = new ArrayList<>();
        TestChecker checker = new TestChecker("1.0", () -> "1.1");
        checker.onSuccess((requesters, latest) -> found.add(requesters[0] + ":" + latest));

        checker.checkEveryXHours(0.5);

        assertEquals(1, checker.timers.size());
        Timer timer = checker.timers.get(0);
        assertEquals(36_000L, timer.delay);
        assertEquals(36_000L, timer.period);
        assertTrue(found.isEmpty());
        timer.task.run();
        assertEquals(List.of("console:1.1"), found);
        assertEquals(List.of(UpdateCheckSuccess.SUCCESS), checker.events);
    }

    @Test
    void reschedulingCancelsThePreviousTimerAndZeroStopsChecking() {
        TestChecker checker = new TestChecker("1.0", () -> "1.0");
        checker.checkEveryXHours(1);
        checker.checkEveryXHours(2);

        assertEquals(2, checker.timers.size());
        assertTrue(checker.timers.get(0).handle.isCancelled());
        assertFalse(checker.timers.get(1).handle.isCancelled());
        assertEquals(144_000L, checker.timers.get(1).period);

        checker.checkEveryXHours(0);
        assertTrue(checker.timers.get(1).handle.isCancelled());
        assertEquals(2, checker.timers.size());
        checker.stop();
    }

    @Test
    void closedCheckersRefuseNewChecks() {
        TestChecker checker = new TestChecker("1.0", () -> "1.1");
        checker.close();
        assertThrows(IllegalStateException.class, checker::checkNow);
    }

    @Test
    void linksFollowTheSource() {
        TestChecker spigot = new TestChecker("1.0", UpdateCheckSource.SPIGOT, "77");
        spigot.setDownloadLink(77).setChangelogLink(77);
        assertEquals(List.of("https://www.spigotmc.org/resources/77"), spigot.getAppropriateDownloadLinks());
        assertEquals("https://www.spigotmc.org/resources/77/history", spigot.getChangelogLink());

        TestChecker hangar = new TestChecker("1.0", UpdateCheckSource.HANGAR, "me/project/release");
        hangar.setDownloadLink(5).setPaidDownloadLink("paid").setFreeDownloadLink("free");
        assertEquals(List.of("paid", "free"), hangar.getAppropriateDownloadLinks());
        hangar.setUsingPaidVersion(true);
        assertEquals(List.of("paid"), hangar.getAppropriateDownloadLinks());
        assertThrows(IllegalArgumentException.class, () -> new TestChecker("1.0", UpdateCheckSource.HANGAR, "me/project"));
    }

    @Test
    void coreHelpersReportVersionsAndLinks() {
        assertEquals("1.1", UpdateCheckerCore.latestOrUsed("1.0", "1.1"));
        assertEquals("1.0", UpdateCheckerCore.latestOrUsed("1.0", "0.9"));
        assertEquals(UpdateCheckResult.RUNNING_LATEST_VERSION, UpdateCheckerCore.checkResult("1.0", "1.0"));
        assertNull(new UpdateCheckerCore(UpdateCheckSource.CUSTOM_URL, "https://example.org").resourceLink(1));
        assertFalse(new UpdateCheckerCore((VersionSupplier) null).hasSource());
    }

    private record Timer(long delay, long period, Runnable task, Handle handle) {}

    private static final class TestChecker extends AbstractUpdateChecker<String, TestChecker> {
        private final List<UpdateCheckSuccess> events = new ArrayList<>();
        private final List<Timer> timers = new ArrayList<>();
        private final List<Runnable> deferred = new ArrayList<>();
        private boolean deferAsync;

        private TestChecker(String used, VersionSupplier supplier) {
            super(used, supplier);
        }

        private TestChecker(String used, UpdateCheckSource source, String parameter) {
            super(used, source, parameter);
        }

        @Override
        protected AsyncTaskProvider tasks() {
            return new AsyncTaskProvider() {
                @Override
                public TaskHandle async(Runnable runnable) {
                    if (deferAsync) {
                        deferred.add(runnable);
                    } else {
                        runnable.run();
                    }
                    return new Handle();
                }

                @Override
                public TaskHandle asyncLater(long delay, Runnable runnable) {
                    return new Handle();
                }

                @Override
                public TaskHandle asyncTimer(long delay, long runEvery, Runnable runnable) {
                    Handle handle = new Handle();
                    timers.add(new Timer(delay, runEvery, runnable, handle));
                    return handle;
                }
            };
        }

        @Override
        protected String[] consoleRequesters() {
            return new String[]{"console"};
        }

        @Override
        protected String defaultUserAgent() {
            return "test";
        }

        @Override
        protected void fireEvent(UpdateCheckSuccess success, String[] requesters) {
            events.add(success);
        }
    }

    private static final class Handle implements TaskHandle {
        private boolean cancelled;

        @Override public void cancel() { cancelled = true; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public Object getRaw() { return this; }
    }
}
