package dev.furkan.blockphysics;

import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub "releases/latest" uc noktasini kontrol ederek yeni bir surum olup
 * olmadigini bildirir. Repo public oldugu icin token gerekmez.
 */
public final class UpdateChecker {

    public record RemoteRelease(String version, String url) {
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final BlockPhysicsPlugin plugin;
    private final String repository;

    public UpdateChecker(BlockPhysicsPlugin plugin, String repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /** Asenkron olarak kontrol eder; sonuc (bulunamadi ise null) ana thread'de callback'e verilir. */
    public void checkAsync(Consumer<RemoteRelease> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            RemoteRelease release = fetchLatestRelease();
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(release));
        });
    }

    private RemoteRelease fetchLatestRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repository + "/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "BlockPhysics-UpdateChecker")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                return null;
            }
            if (response.statusCode() != 200) {
                plugin.getLogger().warning("Guncelleme kontrolu basarisiz: HTTP " + response.statusCode());
                return null;
            }

            String body = response.body();
            String tag = extractField(body, "tag_name");
            String url = extractField(body, "html_url");
            if (tag == null) {
                return null;
            }

            String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
            return new RemoteRelease(version, url != null ? url : "https://github.com/" + repository + "/releases");
        } catch (IOException e) {
            plugin.getLogger().warning("Guncelleme kontrolu sirasinda hata: " + e.getMessage());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    private static String extractField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    /** remote surumu current'tan buyukse true doner. Basit "1.2.3" tarzi surumleri karsilastirir. */
    public static boolean isNewer(String remote, String current) {
        String[] r = remote.split("\\.");
        String[] c = current.split("\\.");
        int length = Math.max(r.length, c.length);
        for (int i = 0; i < length; i++) {
            int rv = i < r.length ? parsePart(r[i]) : 0;
            int cv = i < c.length ? parsePart(c[i]) : 0;
            if (rv != cv) {
                return rv > cv;
            }
        }
        return false;
    }

    private static int parsePart(String part) {
        StringBuilder digits = new StringBuilder();
        for (char ch : part.toCharArray()) {
            if (Character.isDigit(ch)) {
                digits.append(ch);
            } else {
                break;
            }
        }
        return digits.isEmpty() ? 0 : Integer.parseInt(digits.toString());
    }
}
