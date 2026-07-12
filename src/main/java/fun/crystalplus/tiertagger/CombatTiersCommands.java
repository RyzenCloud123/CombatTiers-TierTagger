package fun.crystalplus.tiertagger;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.Map;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class CombatTiersCommands {
    private CombatTiersCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess) -> dispatcher.register(
                literal("ctiers")
                    .then(
                        argument("player", StringArgumentType.word())
                            .executes(context -> {
                                String username =
                                    StringArgumentType.getString(context, "player");

                                Map<String, String> tiers =
                                    TierTaggerClient.getPlayerTiers(username);

                                Text profile =
                                    TierText.createPlayerProfile(username, tiers);

                                context.getSource().sendFeedback(profile);
                                return 1;
                            })
                    )
            )
        );
    }
}
