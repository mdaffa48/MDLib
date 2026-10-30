package com.muhammaddaffa.mdlib.core.update;

import com.muhammaddaffa.mdlib.core.task.AsyncTaskProvider;
import com.muhammaddaffa.mdlib.core.task.TaskHandle;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckResult;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckSource;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckSuccess;
import com.muhammaddaffa.mdlib.updatechecker.UpdateCheckerCore;
import com.muhammaddaffa.mdlib.updatechecker.VersionSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

@SuppressWarnings("UnusedReturnValue")
public abstract class AbstractUpdateChecker<S, C extends AbstractUpdateChecker<S, C>> {

    private final String spigotUserId = "%%__USER__%%";
    private final UpdateCheckerCore core;
    private String changelogLink = null;
    private boolean checkedAtLeastOnce = false;
    private boolean coloredConsoleOutput = false;
    private String donationLink = null;
    private String freeDownloadLink = null;
    private volatile String latestVersion = null;
    private String nameFreeVersion = "Free";
    private String namePaidVersion = "Paid";
    private String notifyPermission = null;
    private boolean notifyRequesters = true;
    private String supportLink = null;
    private boolean suppressUpToDateMessage = true;
    private BiConsumer<S[], Exception> onFail = (requesters, ex) -> ex.printStackTrace();
    private BiConsumer<S[], String> onSuccess = (requesters, latestVersion) -> {
    };
    private String paidDownloadLink = null;
    @Nullable
    private TaskHandle updaterTask = null;
    private int timeout = 0;
    private String usedVersion;
    private String userAgentString = null;
    private volatile boolean closed;
    private boolean usingPaidVersion = false;

    /**
     * Checks {@code usedVersion} against the version a custom {@link VersionSupplier} returns.
     */
    protected AbstractUpdateChecker(@NotNull String usedVersion, @NotNull VersionSupplier supplier) {
        this(usedVersion, new UpdateCheckerCore(supplier));
    }

    /**
     * Checks {@code usedVersion} against {@code updateCheckSource}. See {@link UpdateCheckSource} for the parameter each source needs.
     */
    protected AbstractUpdateChecker(@NotNull String usedVersion, @NotNull UpdateCheckSource updateCheckSource, @NotNull String parameter) {
        this(usedVersion, new UpdateCheckerCore(updateCheckSource, parameter));
    }

    private AbstractUpdateChecker(String usedVersion, UpdateCheckerCore core) {
        this.core = core;
        this.usedVersion = usedVersion.trim();
        this.usingPaidVersion = spigotUserId.matches("^[0-9]+$");
    }

    /**
     * The scheduler the checks run on.
     */
    protected abstract AsyncTaskProvider tasks();

    /**
     * The requesters a scheduled check reports to, normally just the console.
     */
    protected abstract S[] consoleRequesters();

    /**
     * The User-Agent sent when none was set with {@link #setUserAgent(String)}.
     */
    protected abstract String defaultUserAgent();

    /**
     * Fires the platform's UpdateCheckEvent once a check has finished.
     */
    protected abstract void fireEvent(UpdateCheckSuccess success, S[] requesters);

    @SuppressWarnings("unchecked")
    private C self() {
        return (C) this;
    }

    /**
     * Marks this checker as shut down and stops the scheduled checks. Checks still running are dropped.
     */
    protected void close() {
        closed = true;
        stop();
    }

    public boolean isSuppressUpToDateMessage() {
        return suppressUpToDateMessage;
    }

    /**
     * Starts to check every X hours for updates. If a task is already running, it
     * gets cancelled and replaced with the new one. The first check happens after X hours,
     * so you might want to call checkNow() too.
     */
    public C checkEveryXHours(double hours) {
        double minutes = hours * 60;
        double seconds = minutes * 60;
        long ticks = ((int) seconds) * 20L;
        stop();
        if (ticks > 0) {
            updaterTask = tasks().asyncTimer(ticks, ticks, () -> checkNow(consoleRequesters()));
        } else {
            updaterTask = null;
        }
        return self();
    }

    /**
     * Stops the scheduled update checks.
     */
    public C stop() {
        if (updaterTask != null) {
            updaterTask.cancel();
        }
        updaterTask = null;
        return self();
    }

    /**
     * Checks for updates now and sends the result to the given requesters. Can be null to silently check for updates.
     */
    @SafeVarargs
    public final C checkNow(@Nullable S... requesters) {
        if (closed) {
            throw new IllegalStateException("Update checker is shut down.");
        }
        if (!core.hasSource()) {
            throw new IllegalStateException("API Link has not been set and no supplier was provided.");
        }

        checkedAtLeastOnce = true;

        if (userAgentString == null) {
            userAgentString = defaultUserAgent();
        }

        tasks().async(() -> {
            if (closed) {
                return;
            }

            UpdateCheckSuccess success;
            try {
                String fetched = core.fetchLatestVersion(userAgentString, timeout);
                if (closed) return;
                latestVersion = UpdateCheckerCore.latestOrUsed(usedVersion, fetched);
                success = UpdateCheckSuccess.SUCCESS;
            } catch (final IOException exception) {
                if (closed) {
                    return;
                }
                success = UpdateCheckSuccess.FAIL;
                getOnFail().accept(requesters, exception);
            }

            if (closed) {
                return;
            }
            if (success == UpdateCheckSuccess.SUCCESS) {
                getOnSuccess().accept(requesters, latestVersion);
            }
            if (closed) {
                return;
            }
            fireEvent(success, requesters);
        });
        return self();
    }

    /**
     * Checks for updates now and reports the result to the console.
     */
    public C checkNow() {
        return checkNow(consoleRequesters());
    }

    public boolean isUsingLatestVersion() {
        return usedVersion.equals(latestVersion);
    }

    public BiConsumer<S[], Exception> getOnFail() {
        return onFail;
    }

    public BiConsumer<S[], String> getOnSuccess() {
        return onSuccess;
    }

    /**
     * Zero to two download links. If the list contains two links, the first element is the paid download link.
     */
    public List<String> getAppropriateDownloadLinks() {
        return UpdateCheckerCore.appropriateDownloadLinks(usingPaidVersion, paidDownloadLink, freeDownloadLink);
    }

    public String getChangelogLink() {
        return changelogLink;
    }

    /**
     * Sets the changelog link to the SpigotMC or Polymart history page of {@code resourceId}. Does nothing for other sources.
     */
    public C setChangelogLink(int resourceId) {
        String link = core.changelogLink(resourceId);
        return link == null ? self() : setChangelogLink(link);
    }

    public C setChangelogLink(@Nullable String link) {
        changelogLink = link;
        return self();
    }

    @Nullable
    public String getSupportLink() {
        return supportLink;
    }

    @NotNull
    public C setSupportLink(@Nullable String link) {
        this.supportLink = link;
        return self();
    }

    public String getDonationLink() {
        return donationLink;
    }

    public C setDonationLink(@Nullable String donationLink) {
        this.donationLink = donationLink;
        return self();
    }

    public UpdateCheckResult getLastCheckResult() {
        return UpdateCheckerCore.checkResult(usedVersion, latestVersion);
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public String getNameFreeVersion() {
        return nameFreeVersion;
    }

    public C setNameFreeVersion(String nameFreeVersion) {
        this.nameFreeVersion = nameFreeVersion;
        return self();
    }

    public String getNamePaidVersion() {
        return namePaidVersion;
    }

    public C setNamePaidVersion(String namePaidVersion) {
        this.namePaidVersion = namePaidVersion;
        return self();
    }

    public @Nullable String getNotifyPermission() {
        return notifyPermission;
    }

    public String getSpigotUserId() {
        return spigotUserId;
    }

    public String getUsedVersion() {
        return usedVersion;
    }

    public C setUsedVersion(String usedVersion) {
        this.usedVersion = usedVersion;
        return self();
    }

    public boolean isCheckedAtLeastOnce() {
        return checkedAtLeastOnce;
    }

    public boolean isColoredConsoleOutput() {
        return coloredConsoleOutput;
    }

    public C setColoredConsoleOutput(boolean coloredConsoleOutput) {
        this.coloredConsoleOutput = coloredConsoleOutput;
        return self();
    }

    public boolean isNotifyRequesters() {
        return notifyRequesters;
    }

    public C setNotifyRequesters(boolean notify) {
        notifyRequesters = notify;
        return self();
    }

    public boolean isUsingPaidVersion() {
        return usingPaidVersion;
    }

    public C setUsingPaidVersion(boolean paidVersion) {
        usingPaidVersion = paidVersion;
        return self();
    }

    public C onFail(BiConsumer<S[], Exception> onFail) {
        this.onFail = onFail == null ? (requesters, ex) -> ex.printStackTrace() : onFail;
        return self();
    }

    public C onSuccess(BiConsumer<S[], String> onSuccess) {
        this.onSuccess = onSuccess == null ? (requesters, latestVersion) -> {
        } : onSuccess;
        return self();
    }

    /**
     * Sets the download link to the SpigotMC or Polymart page of {@code resourceId}. Does nothing for other sources.
     */
    public C setDownloadLink(int resourceId) {
        String link = core.resourceLink(resourceId);
        return link == null ? self() : setDownloadLink(link);
    }

    public C setDownloadLink(@Nullable String downloadLink) {
        this.paidDownloadLink = null;
        this.freeDownloadLink = downloadLink;
        return self();
    }

    public C suppressUpToDateMessage(boolean suppress) {
        this.suppressUpToDateMessage = suppress;
        return self();
    }

    /**
     * Sets the free download link to the SpigotMC or Polymart page of {@code resourceId}. Does nothing for other sources.
     */
    public C setFreeDownloadLink(int resourceId) {
        String link = core.resourceLink(resourceId);
        return link == null ? self() : setFreeDownloadLink(link);
    }

    public C setFreeDownloadLink(@Nullable String freeDownloadLink) {
        this.freeDownloadLink = freeDownloadLink;
        return self();
    }

    /**
     * Players with this permission are told about a new version when they join. Null disables the notice.
     */
    public C setNotifyByPermissionOnJoin(@Nullable String permission) {
        notifyPermission = permission;
        return self();
    }

    /**
     * Sets the paid download link to the SpigotMC or Polymart page of {@code resourceId}. Does nothing for other sources.
     */
    public C setPaidDownloadLink(int resourceId) {
        String link = core.resourceLink(resourceId);
        return link == null ? self() : setPaidDownloadLink(link);
    }

    public C setPaidDownloadLink(@NotNull String link) {
        paidDownloadLink = link;
        return self();
    }

    public C setTimeout(int timeout) {
        this.timeout = timeout;
        return self();
    }

    public C setUserAgent(@Nullable String userAgent) {
        userAgentString = userAgent;
        return self();
    }

}
