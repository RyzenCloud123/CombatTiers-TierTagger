package fun.crystalplus.tiertagger;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TierText {
    private static final Map<String, String> ICONS = createIcons();

    private static final List<String> GAMEMODE_ORDER = List.of(
        "vanilla",
        "uhc",
        "pot",
        "nethop",
        "smp",
        "sword",
        "axe",
        "mace",
        "cart"
    );

    private static final List<String> TIER_PRIORITY = List.of(
        "HT1", "LT1",
        "HT2", "LT2",
        "HT3", "LT3",
        "HT4", "LT4",
        "HT5", "LT5",
        "ALT1", "ALT2", "ALT3", "ALT4", "ALT5"
    );

    private TierText() {
    }

    public record BestTier(String gamemode, String tier) {
    }

    public static Text decorateTabName(
        Text originalName,
        Map<String, String> tiers
    ) {
        BestTier best = findBestTier(tiers);
        if (best == null) {
            return originalName;
        }

        MutableText result = Text.empty();

        result.append(icon(best.gamemode()));
        result.append(Text.literal(" "));
        result.append(tier(best.tier()));
        result.append(Text.literal(" | "));
        result.append(originalName.copy());

        return result;
    }

    public static Text createPlayerProfile(
        String username,
        Map<String, String> tiers
    ) {
        MutableText result = Text.empty();

        result.append(icon("overall"));
        result.append(Text.literal(" CombatTiers — " + username + "\n")
            .setStyle(Style.EMPTY.withBold(true).withColor(TextColor.fromRgb(0xF4F4F5))));

        if (tiers == null || tiers.isEmpty()) {
            result.append(Text.literal("No tiers found.")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xEF4444))));
            return result;
        }

        for (String gamemode : GAMEMODE_ORDER) {
            String playerTier = tiers.get(gamemode);
            if (!isValidTier(playerTier)) {
                continue;
            }

            result.append(icon(gamemode));
            result.append(Text.literal(" "));
            result.append(Text.literal(displayName(gamemode) + ": ")
                .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xD4D4D8))));
            result.append(tier(playerTier));
            result.append(Text.literal("\n"));
        }

        return result;
    }

    public static BestTier findBestTier(Map<String, String> tiers) {
        if (tiers == null || tiers.isEmpty()) {
            return null;
        }

        BestTier best = null;
        int bestTierIndex = Integer.MAX_VALUE;
        int bestGamemodeIndex = Integer.MAX_VALUE;

        for (Map.Entry<String, String> entry : tiers.entrySet()) {
            if (!isValidTier(entry.getValue())) {
                continue;
            }

            String normalizedTier =
                entry.getValue().trim().toUpperCase(Locale.ROOT);

            int tierIndex = TIER_PRIORITY.indexOf(normalizedTier);
            if (tierIndex < 0) {
                tierIndex = TIER_PRIORITY.size();
            }

            int gamemodeIndex = GAMEMODE_ORDER.indexOf(entry.getKey());
            if (gamemodeIndex < 0) {
                gamemodeIndex = GAMEMODE_ORDER.size();
            }

            if (
                best == null
                    || tierIndex < bestTierIndex
                    || (
                        tierIndex == bestTierIndex
                            && gamemodeIndex < bestGamemodeIndex
                    )
            ) {
                best = new BestTier(entry.getKey(), normalizedTier);
                bestTierIndex = tierIndex;
                bestGamemodeIndex = gamemodeIndex;
            }
        }

        return best;
    }

    public static Text icon(String gamemode) {
        String glyph = ICONS.getOrDefault(gamemode, "");
        return Text.literal(glyph)
            .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF)));
    }

    public static Text tier(String tier) {
        String normalized =
            tier == null ? "" : tier.trim().toUpperCase(Locale.ROOT);

        return Text.literal(normalized)
            .setStyle(
                Style.EMPTY
                    .withBold(true)
                    .withColor(TextColor.fromRgb(tierColor(normalized)))
            );
    }

    private static boolean isValidTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return false;
        }

        String normalized = tier.trim().toUpperCase(Locale.ROOT);
        return !normalized.equals("NONE")
            && !normalized.equals("NULL")
            && !normalized.equals("N/A");
    }

    private static int tierColor(String tier) {
        return switch (tier) {
            case "HT1" -> 0xF2B51D;
            case "LT1" -> 0xEF4444;
            case "HT2" -> 0xF59E0B;
            case "LT2" -> 0xF97316;
            case "HT3" -> 0xF2B51D;
            case "LT3" -> 0xFF3B4E;
            case "HT4" -> 0xA855F7;
            case "LT4" -> 0x8B5CF6;
            case "HT5" -> 0x22D3EE;
            case "LT5" -> 0x34D399;
            case "ALT1", "ALT2", "ALT3", "ALT4", "ALT5" -> 0x93C5FD;
            default -> 0xFFFFFF;
        };
    }

    private static String displayName(String gamemode) {
        return switch (gamemode) {
            case "vanilla" -> "Vanilla";
            case "nethop" -> "NethPot";
            case "smp" -> "SMP";
            case "sword" -> "Sword";
            case "axe" -> "Axe";
            case "mace" -> "Mace";
            case "pot" -> "Pot";
            case "cart" -> "Cart";
            case "uhc" -> "UHC";
            default -> gamemode;
        };
    }

    private static Map<String, String> createIcons() {
        Map<String, String> icons = new LinkedHashMap<>();
        icons.put("vanilla", "\uE001");
        icons.put("nethop", "\uE002");
        icons.put("smp", "\uE003");
        icons.put("sword", "\uE004");
        icons.put("axe", "\uE005");
        icons.put("mace", "\uE006");
        icons.put("pot", "\uE007");
        icons.put("cart", "\uE008");
        icons.put("uhc", "\uE009");
        icons.put("overall", "\uE00A");
        return icons;
    }
}
