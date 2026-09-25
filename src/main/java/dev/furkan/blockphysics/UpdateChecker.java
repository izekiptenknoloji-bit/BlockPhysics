package dev.furkan.blockphysics;

import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub "releases/latest" uc noktasini kontrol ederek yeni bir surum olup
 * olmadigini bildirir ve istenirse jar'i indirip Bukkit'in "update folder"
 * mekanizmasiyla bir sonraki sunucu yeniden baslatildiginda otomatik
 * kurulacak sekilde hazirlar. Repo public oldugu icin token gerekmez.
 */
public final class UpdateChecker {

    public record RemoteRelease(String version, String url, String jarDownloadUrl) {
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final HttpClient DOWNLOAD_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
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

    /**
     * Verilen release'in jar'ini indirip Bukkit'in update-folder'ina, su an
     * yuklu olan plugin jar'iyla AYNI dosya adiyla yazar. Sunucu bir sonraki
     * acilista bu dosyayi otomatik olarak plugins/ klasorune tasiyip eski
     * jar'in yerine koyar (Bukkit/Paper'in yerlesik guncelleme mekanizmasi).
     * Sonuc (basarili mi) ana thread'de callback'e verilir.
     */
    public void downloadAndStage(RemoteRelease release, Consumer<Boolean> callback) {
        if (release.jarDownloadUrl() == null) {
            plugin.getLogger().warning("Yeni surumde indirilebilir bir .jar dosyasi bulunamadi.");
            callback.accept(false);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = downloadToUpdateFolder(release.jarDownloadUrl());
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(success));
        });
    }

    private boolean downloadToUpdateFolder(String downloadUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .header("User-Agent", "BlockPhysics-UpdateChecker")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = DOWNLOAD_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                plugin.getLogger().warning("Guncelleme indirilemedi: HTTP " + response.statusCode());
                return false;
            }

            byte[] body = response.body();
            if (body.length < 1024 || body[0] != 'P' || body[1] != 'K') {
                plugin.getLogger().warning("Indirilen dosya gecerli bir jar dosyasina benzemiyor, iptal edildi.");
                return false;
            }

            File updateFolder = plugin.getServer().getUpdateFolderFile();
            if (!updateFolder.exists() && !updateFolder.mkdirs()) {
                plugin.getLogger().warning("Guncelleme klasoru olusturulamadi: " + updateFolder.getAbsolutePath());
                return false;
            }

            File targetFile = new File(updateFolder, plugin.getPluginJarFile().getName());
            Files.write(targetFile.toPath(), body);
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("Guncelleme indirme hatasi: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
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
            String jarUrl = extractJarAssetUrl(body);
            if (tag == null) {
                return null;
            }

            String version = tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag;
            return new RemoteRelease(version, url != null ? url : "https://github.com/" + repository + "/releases", jarUrl);
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

    private static String extractJarAssetUrl(String json) {
        Matcher matcher = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"").matcher(json);
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
