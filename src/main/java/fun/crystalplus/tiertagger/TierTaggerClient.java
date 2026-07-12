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
import java.util.Collections;
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

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(7))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private static final ScheduledExecutorService EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(
                runnable,
                "CombatTiers-TierTagger-Database"
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
            TierTaggerClient::refreshDatabase,
            0,
            15,
            TimeUnit.SECONDS
        );
    }

    /**
     * Returns every available gamemode tier for a player.
     */
    public static Map<String, String> getPlayerTiers(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyMap();
        }

        String normalizedUsername =
            username.trim().toLowerCase(Locale.ROOT);

        Map<String, String> tiers =
            PLAYER_TIERS.get(normalizedUsername);

        if (tiers == null || tiers.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(
            new LinkedHashMap<>(tiers)
        );
    }

    /**
     * Optional helper for one specific gamemode.
     */
    public static String getTier(
        String username,
        String gamemode
    ) {
        if (
            username == null
                || username.isBlank()
                || gamemode == null
                || gamemode.isBlank()
        ) {
            return null;
        }

        Map<String, String> tiers =
            getPlayerTiers(username);

        return tiers.get(
            normalizeGamemode(gamemode)
        );
    }

    private static void refreshDatabase() {
        if (!REQUEST_RUNNING.compareAndSet(false, true)) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(FIREBASE_URL)
            )
            .timeout(Duration.ofSeconds(12))
            .header("Accept", "application/json")
            .header(
                "User-Agent",
                "CombatTiers-TierTagger/1.1.0"
            )
            .GET()
            .build();

        HTTP_CLIENT.sendAsync(
                request,
                HttpResponse.BodyHandlers.ofString(
                    StandardCharsets.UTF_8
                )
            )
            .orTimeout(15, TimeUnit.SECONDS)
            .whenComplete((response, throwable) -> {
                try {
                    if (throwable != null) {
                        System.err.println(
                            "[Combat Tiers TierTagger] Database request failed: "
                                + throwable.getClass().getSimpleName()
                                + ": "
                                + throwable.getMessage()
                        );

                        return;
                    }

                    if (response == null) {
                        System.err.println(
                            "[Combat Tiers TierTagger] Empty database response."
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
                            + " player tier profiles."
                    );

                } catch (RuntimeException exception) {
                    System.err.println(
                        "[Combat Tiers TierTagger] Database parse failed: "
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

        if (json == null || json.isBlank()) {
            return result;
        }

        JsonElement rootElement =
            JsonParser.parseString(json);

        if (
            rootElement == null
                || rootElement.isJsonNull()
                || !rootElement.isJsonObject()
        ) {
            return result;
        }

        JsonObject players =
            rootElement.getAsJsonObject();

        for (
            Map.Entry<String, JsonElement> playerEntry
                : players.entrySet()
        ) {
            JsonElement playerElement =
                playerEntry.getValue();

            if (
                playerElement == null
                    || playerElement.isJsonNull()
                    || !playerElement.isJsonObject()
            ) {
                continue;
            }

            JsonObject player =
                playerElement.getAsJsonObject();

            if (
                !player.has("tiers")
                    || player.get("tiers").isJsonNull()
                    || !player.get("tiers").isJsonObject()
            ) {
                continue;
            }

            JsonObject tiersObject =
                player.getAsJsonObject("tiers");

            Map<String, String> parsedTiers =
                parseTierObject(tiersObject);

            if (parsedTiers.isEmpty()) {
                continue;
            }

            String databaseKey =
                normalizeUsername(playerEntry.getKey());

            if (!databaseKey.isBlank()) {
                result.put(
                    databaseKey,
                    new LinkedHashMap<>(parsedTiers)
                );
            }

            if (
                player.has("username")
                    && !player.get("username").isJsonNull()
            ) {
                String username =
                    normalizeUsername(
                        player.get("username").getAsString()
                    );

                if (!username.isBlank()) {
                    result.put(
                        username,
                        new LinkedHashMap<>(parsedTiers)
                    );
                }
            }
        }

        return result;
    }

    private static Map<String, String> parseTierObject(
        JsonObject tiersObject
    ) {
        Map<String, String> tiers =
            new LinkedHashMap<>();

        for (
            Map.Entry<String, JsonElement> tierEntry
                : tiersObject.entrySet()
        ) {
            JsonElement tierElement =
                tierEntry.getValue();

            if (
                tierElement == null
                    || tierElement.isJsonNull()
                    || !tierElement.isJsonPrimitive()
            ) {
                continue;
            }

            String gamemode =
                normalizeGamemode(tierEntry.getKey());

            String tier = tierElement
                .getAsString()
                .trim()
                .toUpperCase(Locale.ROOT);

            if (
                gamemode.isBlank()
                    || tier.isBlank()
                    || tier.equals("NONE")
                    || tier.equals("NULL")
                    || tier.equals("N/A")
            ) {
                continue;
            }

            tiers.put(gamemode, tier);
        }

        return tiers;
    }

    private static String normalizeUsername(
        String username
    ) {
        if (username == null) {
            return "";
        }

        return username
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private static String normalizeGamemode(
        String gamemode
    ) {
        if (gamemode == null) {
            return "";
        }

        String normalized = gamemode
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("-", "")
            .replace("_", "")
            .replace(" ", "");

        return switch (normalized) {
            case "vanilla", "crystal", "crystalpvp" ->
                "vanilla";

            case "nethop",
                 "nethpot",
                 "netherpot",
                 "netheritepot",
                 "netheriteop" ->
                "nethop";

            case "smp", "smppvp" ->
                "smp";

            case "sword", "swords", "swordpvp" ->
                "sword";

            case "axe", "axepvp" ->
                "axe";

            case "mace", "macepvp" ->
                "mace";

            case "pot",
                 "potion",
                 "potionpvp",
                 "potpvp" ->
                "pot";

            case "cart",
                 "cartpvp",
                 "minecart" ->
                "cart";

            case "uhc" ->
                "uhc";

            default ->
                normalized;
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

            PLAYER_TIERS.clear();
            PLAYER_TIERS.putAll(parsed);

            System.out.println(
                "[Combat Tiers TierTagger] Loaded "
                    + parsed.size()
                    + " cached player profiles."
            );

        } catch (IOException | RuntimeException exception) {
            System.err.println(
                "[Combat Tiers TierTagger] Cache load failed: "
                    + exception.getMessage()
            );
        }
    }

    private static void saveCache(String json) {
        if (json == null || json.isBlank()) {
            return;
        }

        try {
            Files.writeString(
                CACHE_FILE,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );

        } catch (IOException exception) {
            System.err.println(
                "[Combat Tiers TierTagger] Cache save failed: "
                    + exception.getMessage()
            );
        }
    }
}
