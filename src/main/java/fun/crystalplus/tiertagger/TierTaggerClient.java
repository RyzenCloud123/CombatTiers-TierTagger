package fun.crystalplus.tiertagger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TierTaggerClient implements ClientModInitializer {
    public static final String MOD_ID = "combattier_tiertagger";

    private static final String FIREBASE_URL =
        "https://project-5dd9e-default-rtdb.asia-southeast1.firebasedatabase.app/players.json";

    private static final Map<String, String> VANILLA_TIERS = new ConcurrentHashMap<>();
    private static final AtomicBoolean REQUEST_RUNNING = new AtomicBoolean(false);

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static final ScheduledExecutorService EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "CombatTiers-TierTagger");
            thread.setDaemon(true);
            return thread;
        });

    private static final Path CACHE_FILE = Path.of(
        System.getProperty("user.home"),
        ".combattiers-tiertagger-cache.json"
    );

    @Override
    public void onInitializeClient() {
        System.out.println("[Combat Tiers TierTagger] Starting native 1.21.11 build.");
        loadCache();

        EXECUTOR.scheduleWithFixedDelay(
            TierTaggerClient::refresh,
            0,
            15,
            TimeUnit.SECONDS
        );
    }

    public static String getVanillaTier(String username) {
        if (username == null) {
            return null;
        }

        return VANILLA_TIERS.get(username.toLowerCase(Locale.ROOT));
    }

    private static void refresh() {
        if (!REQUEST_RUNNING.compareAndSet(false, true)) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(FIREBASE_URL))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header("User-Agent", "CombatTiers-TierTagger/1.0.0")
            .GET()
            .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            .orTimeout(12, TimeUnit.SECONDS)
            .whenComplete((response, throwable) -> {
                try {
                    if (throwable != null) {
                        System.err.println(
                            "[Combat Tiers TierTagger] Database request failed: "
                                + throwable.getClass().getSimpleName()
                        );
                        return;
                    }

                    if (response.statusCode() != 200) {
                        System.err.println(
                            "[Combat Tiers TierTagger] Database HTTP status: "
                                + response.statusCode()
                        );
                        return;
                    }

                    Map<String, String> parsed = parsePlayers(response.body());
                    VANILLA_TIERS.clear();
                    VANILLA_TIERS.putAll(parsed);
                    saveCache(response.body());

                    System.out.println(
                        "[Combat Tiers TierTagger] Loaded "
                            + parsed.size()
                            + " Vanilla tiers."
                    );
                } catch (Exception exception) {
                    System.err.println(
                        "[Combat Tiers TierTagger] Parse failure: "
                            + exception.getMessage()
                    );
                } finally {
                    REQUEST_RUNNING.set(false);
                }
            });
    }

    private static Map<String, String> parsePlayers(String json) {
        Map<String, String> result = new HashMap<>();

        JsonElement rootElement = JsonParser.parseString(json);
        if (!rootElement.isJsonObject()) {
            return result;
        }

        JsonObject players = rootElement.getAsJsonObject();

        for (Map.Entry<String, JsonElement> playerEntry : players.entrySet()) {
            if (!playerEntry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject player = playerEntry.getValue().getAsJsonObject();
            JsonObject tiers = player.has("tiers") && player.get("tiers").isJsonObject()
                ? player.getAsJsonObject("tiers")
                : null;

            if (tiers == null || !tiers.has("vanilla") || tiers.get("vanilla").isJsonNull()) {
                continue;
            }

            String tier = tiers.get("vanilla").getAsString().trim().toUpperCase(Locale.ROOT);
            if (tier.isEmpty() || tier.equals("NONE")) {
                continue;
            }

            String databaseKey = playerEntry.getKey().toLowerCase(Locale.ROOT);
            result.put(databaseKey, tier);

            if (player.has("username") && !player.get("username").isJsonNull()) {
                String username = player.get("username").getAsString();
                if (!username.isBlank()) {
                    result.put(username.toLowerCase(Locale.ROOT), tier);
                }
            }
        }

        return result;
    }

    private static void loadCache() {
        if (!Files.isRegularFile(CACHE_FILE)) {
            return;
        }

        try {
            String json = Files.readString(CACHE_FILE, StandardCharsets.UTF_8);
            Map<String, String> parsed = parsePlayers(json);
            VANILLA_TIERS.putAll(parsed);

            System.out.println(
                "[Combat Tiers TierTagger] Loaded "
                    + parsed.size()
                    + " cached tiers."
            );
        } catch (IOException | RuntimeException exception) {
            System.err.println(
                "[Combat Tiers TierTagger] Cache load failed: "
                    + exception.getMessage()
            );
        }
    }

    private static void saveCache(String json) {
        try {
            Files.writeString(
                CACHE_FILE,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException exception) {
            System.err.println(
                "[Combat Tiers TierTagger] Cache save failed: "
                    + exception.getMessage()
            );
        }
    }
}
