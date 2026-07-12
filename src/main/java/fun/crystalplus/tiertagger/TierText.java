package fun.crystalplus.tiertagger;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;

public final class TierText {

    private static final String VANILLA_ICON = "\uE001";

    private static final List<String> GAMEMODE_ORDER = List.of(
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
        MutableText result = Text.empty();

        for (String gamemode : GAMEMODE_ORDER) {
            String tier = tiers.get(gamemode);

            if (
                tier == null
                    || tier.isBlank()
                    || tier.equalsIgnoreCase("NONE")
            ) {
                continue;
            }

            if (!result.getString().isEmpty()) {
                result.append(Text.literal(" "));
            }

            if (gamemode.equals("vanilla")) {
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
                Text.literal(getGamemodeShortName(gamemode) + " ")
                    .formatted(Formatting.GRAY)
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

        if (!result.getString().isEmpty()) {
            result.append(Text.literal(" "));
        }

        result.append(originalName.copy());

        return result;
    }

    private static String getGamemodeShortName(String gamemode) {
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
            default -> gamemode.toUpperCase();
        };
    }

    private static Formatting getTierColor(String tier) {
        if (tier.endsWith("1")) {
            return Formatting.RED;
        }

        if (tier.endsWith("2")) {
            return Formatting.GOLD;
        }

        if (tier.endsWith("3")) {
            return Formatting.YELLOW;
        }

        if (tier.endsWith("4")) {
            return Formatting.GREEN;
        }

        if (tier.endsWith("5")) {
            return Formatting.AQUA;
        }

        return Formatting.WHITE;
    }
}
