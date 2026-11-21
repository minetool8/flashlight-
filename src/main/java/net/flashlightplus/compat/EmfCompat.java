package net.flashlightplus.compat;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;
import traben.entity_model_features.EMFAnimationApi;

import java.util.Map;

/**
 * Runs right after EMF has executed a custom (CEM) animation and puts the flashlight
 * hands-up pose back on top, so resource packs like Fresh Animations can't override it.
 */
public final class EmfCompat extends EMFAnimationApi.EMFAnimationHook {

    public static void register() throws Exception {
        EMFAnimationApi.registerAnimationHook(new EmfCompat());
    }

    @Override
    public void onAnimationEnd(AnimationContext ctx, boolean flag) {
        try {
            if (ctx == null || ctx.activeState() == null || ctx.animatingModelRoot() == null) return;
            Object ent = ctx.activeState().emfEntity();
            if (!(ent instanceof Player p)) return;
            Map<?, ?> parts = ctx.animatingModelRoot().getAllVanillaPartsByNameEMF();
            if (parts == null) return;
            ModelPart la = part(parts, "left_arm"), ra = part(parts, "right_arm");
            if (la == null && ra == null) return;
            PoseMath.applyAfterEmf(p, la, ra, part(parts, "left_sleeve"), part(parts, "right_sleeve"));
        } catch (Throwable ignored) {
            // never break rendering
        }
    }

    private static ModelPart part(Map<?, ?> m, String n) {
        Object o = m.get(n);
        return o instanceof ModelPart mp ? mp : null;
    }
}
