package com.muhammaddaffa.mdlib.updatechecker;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckerCoreTest {

    private HttpServer server;
    private final List<String> userAgents = new CopyOnWriteArrayList<>();

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String serve(String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/version", exchange -> {
            userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/version";
    }

    @Test
    void customUrlsSendTheUserAgentAndReadTheTrimmedFirstLine() throws IOException {
        String url = serve("  2.0.1  \nsecond line\n");

        String latest = new UpdateCheckerCore(UpdateCheckSource.CUSTOM_URL, url).fetchLatestVersion("MDLibTest/1", 2000);

        assertEquals("2.0.1", latest);
        assertEquals(List.of("MDLibTest/1"), userAgents);
    }

    @Test
    void suppliersAreUsedInsteadOfHttp() throws IOException {
        assertEquals("9", new UpdateCheckerCore(() -> "9").fetchLatestVersion("ua", 0));
        assertTrue(new UpdateCheckerCore(() -> "9").hasSource());
        assertNull(new UpdateCheckerCore(() -> "9").getUpdateCheckSource());
    }

    @Test
    void jsonMappersReadTheirOwnFields() throws IOException {
        assertEquals("1.4", VersionMapper.SPIGET.apply(reader("{\"name\":\"1.4\",\"id\":7}")));
        assertEquals("1.5", VersionMapper.SPIGOT.apply(reader("{\"current_version\":\"1.5\",\"name\":\"x\"}")));
        assertEquals("v2", VersionMapper.GITHUB_RELEASE_TAG.apply(reader("[{\"tag_name\":\"v2\"},{\"tag_name\":\"v1\"}]")));
        IOException none = assertThrows(IOException.class, () -> VersionMapper.GITHUB_RELEASE_TAG.apply(reader("[]")));
        assertEquals("Could not check for updates: no GitHub release found.", none.getMessage());
    }

    @Test
    void resourceAndChangelogLinksOnlyExistForSpigotAndPolymart() {
        UpdateCheckerCore spigot = new UpdateCheckerCore(UpdateCheckSource.SPIGOT, "12");
        assertEquals("https://www.spigotmc.org/resources/12", spigot.resourceLink(12));
        assertEquals("https://www.spigotmc.org/resources/12/history", spigot.changelogLink(12));
        UpdateCheckerCore polymart = new UpdateCheckerCore(UpdateCheckSource.POLYMART, "3");
        assertEquals("https://polymart.org/resource/3", polymart.resourceLink(3));
        assertEquals("https://polymart.org/resource/3/updates", polymart.changelogLink(3));
        UpdateCheckerCore github = new UpdateCheckerCore(UpdateCheckSource.GITHUB_RELEASE_TAG, "owner/repo");
        assertNull(github.resourceLink(1));
        assertNull(github.changelogLink(1));
        assertEquals(UpdateCheckSource.GITHUB_RELEASE_TAG, github.getUpdateCheckSource());
        for (UpdateCheckerCore other : List.of(new UpdateCheckerCore(UpdateCheckSource.SPIGET, "1"),
                new UpdateCheckerCore(UpdateCheckSource.HANGAR, "me/project/release"),
                new UpdateCheckerCore(UpdateCheckSource.CUSTOM_URL, "https://example.org/v"),
                new UpdateCheckerCore(() -> "1"))) {
            assertNull(other.resourceLink(1));
            assertNull(other.changelogLink(1));
        }
    }

    @Test
    void downloadLinksPutThePaidLinkFirst() {
        assertEquals(List.of("paid", "free"), UpdateCheckerCore.appropriateDownloadLinks(false, "paid", "free"));
        assertEquals(List.of("paid"), UpdateCheckerCore.appropriateDownloadLinks(true, "paid", "free"));
        assertEquals(List.of("free"), UpdateCheckerCore.appropriateDownloadLinks(true, null, "free"));
        assertEquals(List.of(), UpdateCheckerCore.appropriateDownloadLinks(false, null, null));
    }

    private static BufferedReader reader(String text) {
        return new BufferedReader(new StringReader(text));
    }
}
