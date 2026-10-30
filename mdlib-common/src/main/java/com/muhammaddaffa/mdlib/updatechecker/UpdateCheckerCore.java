package com.muhammaddaffa.mdlib.updatechecker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class UpdateCheckerCore {

    private static final String SPIGOT_CHANGELOG_SUFFIX = "/history";
    private static final String SPIGOT_DOWNLOAD_LINK = "https://www.spigotmc.org/resources/";
    private static final String SPIGOT_UPDATE_API = "https://api.spigotmc.org/simple/0.2/index.php?action=getResource&id=%s";
    private static final String POLYMART_CHANGELOG_SUFFIX = "/updates";
    private static final String POLYMART_DOWNLOAD_LINK = "https://polymart.org/resource/";
    private static final String POLYMART_UPDATE_API = "https://api.polymart.org/v1/getResourceInfoSimple/?resource_id=%s&key=version";
    private static final String SPIGET_UPDATE_API = "https://api.spiget.org/v2/resources/%s/versions/latest";
    private static final String GITHUB_RELEASE_API = "https://api.github.com/repos/%s/%s/releases";
    private static final String HANGAR_RELEASE_API =  "https://hangar.papermc.io/api/v1/projects/%s/%s/latest?channel=%s";

    private final UpdateCheckSource updateCheckSource;
    private final String apiLink;
    private final ThrowingFunction<BufferedReader, String, IOException> mapper;
    private final VersionSupplier supplier;

    /**
     * Fetches the latest version through a custom {@link VersionSupplier}.
     */
    public UpdateCheckerCore(VersionSupplier supplier) {
        this.updateCheckSource = null;
        this.apiLink = null;
        this.mapper = null;
        this.supplier = supplier;
    }

    /**
     * Fetches the latest version from {@code updateCheckSource}. See {@link UpdateCheckSource} for the parameter each source needs.
     */
    public UpdateCheckerCore(UpdateCheckSource updateCheckSource, String parameter) {
        final String apiLink;
        final ThrowingFunction<BufferedReader, String, IOException> mapper;

        switch (this.updateCheckSource = updateCheckSource) {
            case CUSTOM_URL: {
                apiLink = parameter;
                mapper = VersionMapper.TRIM_FIRST_LINE;
                break;
            }
            case SPIGOT: {
                apiLink = String.format(SPIGOT_UPDATE_API, parameter);
                mapper = VersionMapper.SPIGOT;
                break;
            }
            case POLYMART: {
                apiLink = String.format(POLYMART_UPDATE_API, parameter);
                mapper = VersionMapper.TRIM_FIRST_LINE;
                break;
            }
            case SPIGET: {
                apiLink = String.format(SPIGET_UPDATE_API, parameter);
                mapper = VersionMapper.SPIGET;
                break;
            }
            case GITHUB_RELEASE_TAG: {
                String[] split = parameter.split("/");
                if (split.length < 2) {
                    throw new IllegalArgumentException("Given GitHub repository must be in the format \"<UserOrOrganizationName>/<RepositoryName>\"");
                }
                apiLink = String.format(GITHUB_RELEASE_API, split[0], split[1]);
                mapper = VersionMapper.GITHUB_RELEASE_TAG;
                break;
            }
            case HANGAR: {
                String[] split = parameter.split("/");
                if (split.length <3) {
                    throw new IllegalArgumentException("Given HangarMC project must be in the format \"<UserOrOrganizationName>/<ProjectName>/<ReleaseChannel>\"");
                }

                apiLink = String.format(HANGAR_RELEASE_API, split[0], split[1], split[2]);
                mapper = VersionMapper.TRIM_FIRST_LINE;
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }

        Objects.requireNonNull(apiLink, "API Link cannot be null.");

        this.apiLink = apiLink;
        this.mapper = mapper;
        this.supplier = null;
    }

    public UpdateCheckSource getUpdateCheckSource() {
        return updateCheckSource;
    }

    /**
     * Whether there is an API link or a {@link VersionSupplier} to fetch from.
     */
    public boolean hasSource() {
        return apiLink != null || supplier != null;
    }

    /**
     * Fetches the latest version string. Blocks on network I/O, so call it off the main thread.
     */
    public String fetchLatestVersion(String userAgent, int timeout) throws IOException {
        if (supplier != null) {
            return supplier.getLatestVersionString();
        }
        final HttpURLConnection httpConnection = (HttpURLConnection) new URL(apiLink).openConnection();
        httpConnection.addRequestProperty("User-Agent", userAgent);
        if (timeout > 0) {
            httpConnection.setConnectTimeout(timeout);
        }
        try (final InputStreamReader input = new InputStreamReader(httpConnection.getInputStream()); final BufferedReader reader = new BufferedReader(input)) {
            return mapper.apply(reader);
        }
    }

    /**
     * Checks whether one version is really newer than another according to the semantic versioning scheme, including letters.
     */
    public static boolean isOtherVersionNewer(String myVersion, String otherVersion) {
        DefaultArtifactVersion used = new DefaultArtifactVersion(myVersion);
        DefaultArtifactVersion latest = new DefaultArtifactVersion(otherVersion);
        return used.compareTo(latest) < 0;
    }

    /**
     * The version to report as latest: the fetched one, or {@code usedVersion} when the fetched one is not newer.
     */
    public static String latestOrUsed(String usedVersion, String fetchedVersion) {
        if (!usedVersion.equals(fetchedVersion) && !isOtherVersionNewer(usedVersion, fetchedVersion)) {
            return usedVersion;
        }
        return fetchedVersion;
    }

    public static UpdateCheckResult checkResult(String usedVersion, String latestVersion) {
        if (latestVersion == null) {
            return UpdateCheckResult.UNKNOWN;
        }
        if (latestVersion.equals(usedVersion)) {
            return UpdateCheckResult.RUNNING_LATEST_VERSION;
        }
        return UpdateCheckResult.NEW_VERSION_AVAILABLE;
    }

    /**
     * Download links in the order {@code UpdateChecker#getAppropriateDownloadLinks()} documents: paid first, then free.
     */
    public static List<String> appropriateDownloadLinks(boolean usingPaidVersion, String paidDownloadLink, String freeDownloadLink) {
        List<String> list = new ArrayList<>();

        if (usingPaidVersion) {
            if (paidDownloadLink != null) {
                list.add(paidDownloadLink);
            } else if (freeDownloadLink != null) {
                list.add(freeDownloadLink);
            }
        } else {
            if (paidDownloadLink != null) {
                list.add(paidDownloadLink);
            }
            if (freeDownloadLink != null) {
                list.add(freeDownloadLink);
            }
        }
        return list;
    }

    /**
     * The SpigotMC or Polymart resource page for {@code resourceId}, or null for other sources.
     */
    public String resourceLink(int resourceId) {
        if (updateCheckSource == UpdateCheckSource.SPIGOT) return SPIGOT_DOWNLOAD_LINK + resourceId;
        if (updateCheckSource == UpdateCheckSource.POLYMART) return POLYMART_DOWNLOAD_LINK + resourceId;
        return null;
    }

    /**
     * The SpigotMC or Polymart changelog page for {@code resourceId}, or null for other sources.
     */
    public String changelogLink(int resourceId) {
        if (updateCheckSource == UpdateCheckSource.SPIGOT) return SPIGOT_DOWNLOAD_LINK + resourceId + SPIGOT_CHANGELOG_SUFFIX;
        if (updateCheckSource == UpdateCheckSource.POLYMART) return POLYMART_DOWNLOAD_LINK + resourceId + POLYMART_CHANGELOG_SUFFIX;
        return null;
    }

}
