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
import java.util.LinkedHashMap;
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

    /*
     * Structure:
     * username -> gamemode -> tier
     *
     * Example:
     * negativetier -> vanilla -> HT3
     * negativetier -> nethop  -> LT3
     */
    private static final Map<String, Map<String, String>> PLAYER_TIERS =
        new ConcurrentHashMap<>();

    private static final AtomicBoolean REQUEST_RUNNING =
        new AtomicBoolean(false);

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static final ScheduledExecutorService EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                runnable,
                "CombatTiers-TierTagger"
            );
            thread.setDaemon(true);
            return thread;
        });

    private static final Path CACHE_FILE = Path.of(
        System.getProperty("user.home"),
        ".combattiers-tiertagger-cache.json"
    );

    @Override
    public void onInitializeClient() {
        System.out.println(
            "[Combat Tiers TierTagger] Starting multi-gamemode build."
        );

        loadCache();

        EXECUTOR.scheduleWithFixedDelay(
            TierTaggerClient::refresh,
            0,
            15,
            TimeUnit.SECONDS
        );
    }

    public static Map<String, String> getPlayerTiers(String username) {
        if (username == null || username.isBlank()) {
            return Map.of();
        }

        Map<String, String> tiers = PLAYER_TIERS.get(
            username.toLowerCase(Locale.ROOT)
        );

        if (tiers == null) {
            return Map.of();
        }

        return new LinkedHashMap<>(tiers);
    }

    private static void refresh() {
        if (!REQUEST_RUNNING.compareAndSet(false, true)) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(FIREBASE_URL)
            )
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                "CombatTiers-TierTagger/1.1.0"
            )
            .GET()
            .build();

        HTTP.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString(
                    StandardCharsets.UTF_8
                )
            )
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

                    Map<String, Map<String, String>> parsed =
                        parsePlayers(response.body());

                    PLAYER_TIERS.clear();
                    PLAYER_TIERS.putAll(parsed);

                    saveCache(response.body());

                    System.out.println(
                        "[Combat Tiers TierTagger] Loaded "
                            + parsed.size()
                            + " players with multi-gamemode tiers."
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

    private static Map<String, Map<String, String>> parsePlayers(
        String json
    ) {
        Map<String, Map<String, String>> result =
            new LinkedHashMap<>();

        JsonElement rootElement = JsonParser.parseString(json);

        if (!rootElement.isJsonObject()) {
            return result;
        }

        JsonObject players = rootElement.getAsJsonObject();

        for (
            Map.Entry<String, JsonElement> playerEntry
                : players.entrySet()
        ) {
            if (!playerEntry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject player =
                playerEntry.getValue().getAsJsonObject();

            if (
                !player.has("tiers")
                    || !player.get("tiers").isJsonObject()
            ) {
                continue;
            }

            JsonObject tiersObject =
                player.getAsJsonObject("tiers");

            Map<String, String> tiers =
                new LinkedHashMap<>();

            for (
                Map.Entry<String, JsonElement> tierEntry
                    : tiersObject.entrySet()
            ) {
                if (
                    tierEntry.getValue() == null
                        || tierEntry.getValue().isJsonNull()
                ) {
                    continue;
                }

                String gamemode = tierEntry.getKey()
                    .trim()
                    .toLowerCase(Locale.ROOT);

                String tier = tierEntry.getValue()
                    .getAsString()
                    .trim()
                    .toUpperCase(Locale.ROOT);

                if (
                    tier.isEmpty()
                        || tier.equals("NONE")
                        || tier.equals("NULL")
                ) {
                    continue;
                }

                tiers.put(
                    normalizeGamemode(gamemode),
                    tier
                );
            }

            if (tiers.isEmpty()) {
                continue;
            }

            String databaseKey = playerEntry.getKey()
                .toLowerCase(Locale.ROOT);

            result.put(
                databaseKey,
                new LinkedHashMap<>(tiers)
            );

            if (
                player.has("username")
                    && !player.get("username").isJsonNull()
            ) {
                String username = player.get("username")
                    .getAsString()
                    .trim();

                if (!username.isBlank()) {
                    result.put(
                        username.toLowerCase(Locale.ROOT),
                        new LinkedHashMap<>(tiers)
                    );
                }
            }
        }

        return result;
    }

    private static String normalizeGamemode(String gamemode) {
        return switch (gamemode) {
            case "nethpot", "netherpot", "nethop" -> "nethop";
            case "sword", "swords" -> "sword";
            case "smp" -> "smp";
            case "vanilla" -> "vanilla";
            case "mace" -> "mace";
            case "axe" -> "axe";
            case "pot", "potion" -> "pot";
            case "cart", "cartpvp" -> "cart";
            case "uhc" -> "uhc";
            default -> gamemode;
        };
    }

    private static void loadCache() {
        if (!Files.isRegularFile(CACHE_FILE)) {
            return;
        }

        try {
            String json = Files.readString(
                CACHE_FILE,
                StandardCharsets.UTF_8
            );

            Map<String, Map<String, String>> parsed =
                parsePlayers(json);

            PLAYER_TIERS.putAll(parsed);

            System.out.println(
                "[Combat Tiers TierTagger] Loaded "
                    + parsed.size()
                    + " cached players."
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
