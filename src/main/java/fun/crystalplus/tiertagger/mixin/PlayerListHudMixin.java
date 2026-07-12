package fun.crystalplus.tiertagger.mixin;

import fun.crystalplus.tiertagger.TierTaggerClient;
import fun.crystalplus.tiertagger.TierText;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(
        method = "getPlayerName",
        at = @At("RETURN"),
        cancellable = true
    )
    private void combatTiers$decorateTabName(
        PlayerListEntry entry,
        CallbackInfoReturnable<Text> cir
    ) {
        String username = entry.getProfile().name();

        Map<String, String> tiers =
            TierTaggerClient.getPlayerTiers(username);

        if (tiers.isEmpty()) {
            return;
        }

        cir.setReturnValue(
            TierText.decorate(
                cir.getReturnValue(),
                tiers
            )
        );
    }
}
