package net.flashlightplus.mixin;

import net.flashlightplus.compat.ParkourState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/** While ParCool animates the player (vault, roll, ...) let the arms follow ParCool instead of the hands-up pose. */
@Mixin(targets = "com.keerdm.flashlightmod.client.render.FlashlightArmBlendManager", remap = false)
public abstract class ArmBlendMixin {
    @Inject(method = "getRaisedBlend", at = @At("RETURN"), cancellable = true, remap = false)
    private static void flashlightplus$yieldToParkour(UUID id, float partial, CallbackInfoReturnable<Float> cir) {
        float w = ParkourState.weight(id);
        if (w > 0.001f) cir.setReturnValue(cir.getReturnValueF() * (1f - w));
    }
}
