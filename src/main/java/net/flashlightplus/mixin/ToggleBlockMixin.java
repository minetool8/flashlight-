package net.flashlightplus.mixin;

import net.flashlightplus.compat.Refl;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops the flashlight being switched on from the off hand while a big gun is in the main hand. */
@Mixin(targets = "com.keerdm.flashlightmod.event.FlashlightToggleHandler", remap = false)
public abstract class ToggleBlockMixin {
    @Inject(method = "toggleFlashlight", at = @At("HEAD"), cancellable = true, remap = false)
    private static void flashlightplus$blockOffhand(Player player, CallbackInfo ci) {
        if (player == null || player.level().isClientSide) return;
        if (Refl.offhandBlockedByBigGun(player)) {
            player.displayClientMessage(Component.literal("Can't use a flashlight in your off hand while holding a two-handed gun"), true);
            ci.cancel();
        }
    }
}
