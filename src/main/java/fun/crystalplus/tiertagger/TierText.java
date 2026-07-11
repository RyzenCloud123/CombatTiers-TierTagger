package fun.crystalplus.tiertagger;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TierText {
    private static final String VANILLA_ICON = "\uE001";

    private TierText() {
    }

    public static Text decorate(String username, String tier, Text originalName) {
        Formatting tierColor = tier.startsWith("HT")
            ? Formatting.LIGHT_PURPLE
            : Formatting.AQUA;

        MutableText result = Text.literal(VANILLA_ICON + " ")
            .formatted(Formatting.WHITE);

        result.append(
            Text.literal("[" + tier + "] ")
                .formatted(tierColor)
        );

        result.append(originalName.copy());
        return result;
    }
}
