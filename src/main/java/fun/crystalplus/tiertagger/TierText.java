package fun.crystalplus.tiertagger;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TierText {

    /*
     * Existing custom Vanilla bitmap glyph.
     * This will only render if your font JSON and PNG are present.
     */
    private static final String VANILLA_ICON =
        "\uE001";

    private static final List<String> GAMEMODE_ORDER =
        List.of(
            "vanilla",
            "nethop",
            "smp",
            "sword",
            "axe",
            "mace",
            "pot",
            "cart",
            "uhc"
        );

    private TierText() {
    }

    public static Text decorate(
        Text originalName,
        Map<String, String> tiers
    ) {
        if (tiers == null || tiers.isEmpty()) {
            return originalName;
        }

        MutableText result = Text.empty();
        boolean hasDisplayedTier = false;

        for (String gamemode : GAMEMODE_ORDER) {
            String tier = tiers.get(gamemode);

            if (!isValidTier(tier)) {
                continue;
            }

            if (hasDisplayedTier) {
                result.append(
                    Text.literal(" ")
                );
            }

            appendGamemodeTier(
                result,
                gamemode,
                tier
            );

            hasDisplayedTier = true;
        }

        /*
         * Also display unknown/new gamemodes that are not
         * already present in GAMEMODE_ORDER.
         */
        for (
            Map.Entry<String, String> entry
                : tiers.entrySet()
        ) {
            String gamemode = entry.getKey();
            String tier = entry.getValue();

            if (
                GAMEMODE_ORDER.contains(gamemode)
                    || !isValidTier(tier)
            ) {
                continue;
            }

            if (hasDisplayedTier) {
                result.append(
                    Text.literal(" ")
                );
            }

            appendGamemodeTier(
                result,
                gamemode,
                tier
            );

            hasDisplayedTier = true;
        }

        if (!hasDisplayedTier) {
            return originalName;
        }

        result.append(
            Text.literal(" ")
        );

        if (originalName != null) {
            result.append(
                originalName.copy()
            );
        }

        return result;
    }

    private static void appendGamemodeTier(
        MutableText result,
        String gamemode,
        String tier
    ) {
        if ("vanilla".equals(gamemode)) {
            result.append(
                Text.literal(VANILLA_ICON + " ")
                    .formatted(Formatting.WHITE)
            );
        }

        result.append(
            Text.literal("[")
                .formatted(Formatting.DARK_GRAY)
        );

        result.append(
            Text.literal(
                getGamemodeShortName(gamemode) + " "
            ).formatted(
                getGamemodeColor(gamemode)
            )
        );

        result.append(
            Text.literal(tier)
                .formatted(getTierColor(tier))
        );

        result.append(
            Text.literal("]")
                .formatted(Formatting.DARK_GRAY)
        );
    }

    private static boolean isValidTier(
        String tier
    ) {
        if (tier == null || tier.isBlank()) {
            return false;
        }

        String normalized =
            tier.trim().toUpperCase(Locale.ROOT);

        return !normalized.equals("NONE")
            && !normalized.equals("NULL")
            && !normalized.equals("N/A");
    }

    private static String getGamemodeShortName(
        String gamemode
    ) {
        if (gamemode == null) {
            return "?";
        }

        return switch (gamemode) {
            case "vanilla" -> "V";
            case "nethop" -> "NP";
            case "smp" -> "SMP";
            case "sword" -> "SW";
            case "axe" -> "AX";
            case "mace" -> "MC";
            case "pot" -> "POT";
            case "cart" -> "CT";
            case "uhc" -> "UHC";
            default -> gamemode
                .toUpperCase(Locale.ROOT);
        };
    }

    private static Formatting getGamemodeColor(
        String gamemode
    ) {
        if (gamemode == null) {
            return Formatting.GRAY;
        }

        return switch (gamemode) {
            case "vanilla" ->
                Formatting.LIGHT_PURPLE;

            case "nethop" ->
                Formatting.DARK_PURPLE;

            case "smp" ->
                Formatting.DARK_GREEN;

            case "sword" ->
                Formatting.AQUA;

            case "axe" ->
                Formatting.BLUE;

            case "mace" ->
                Formatting.DARK_GRAY;

            case "pot" ->
                Formatting.LIGHT_PURPLE;

            case "cart" ->
                Formatting.RED;

            case "uhc" ->
                Formatting.GOLD;

            default ->
                Formatting.GRAY;
        };
    }

    private static Formatting getTierColor(
        String tier
    ) {
        if (tier == null) {
            return Formatting.WHITE;
        }

        String normalized =
            tier.trim().toUpperCase(Locale.ROOT);

        if (normalized.endsWith("1")) {
            return Formatting.RED;
        }

        if (normalized.endsWith("2")) {
            return Formatting.GOLD;
        }

        if (normalized.endsWith("3")) {
            return Formatting.YELLOW;
        }

        if (normalized.endsWith("4")) {
            return Formatting.GREEN;
        }

        if (normalized.endsWith("5")) {
            return Formatting.AQUA;
        }

        return normalized.startsWith("HT")
            ? Formatting.LIGHT_PURPLE
            : Formatting.WHITE;
    }
}
