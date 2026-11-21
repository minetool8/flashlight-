package net.flashlightplus.compat;

import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;

import java.util.UUID;

/** Re-implementation of Flashier Flashlights' hands-up arm pose (constants read from its bytecode). */
public final class PoseMath {
    private PoseMath() {}

    static float poseAmount(Player p, float partial) {
        if (p.isAutoSpinAttack()) return 1f;
        if (p.isFallFlying()) {
            float t = p.getFallFlyingTicks() + partial;
            return Mth.clamp(t * t / 100f, 0f, 1f);
        }
        return p.getSwimAmount(partial);
    }

    /** {xRot, yRot, zRot} in radians of the fully-raised flashlight pose. */
    public static float[] flashlightRot(Player p, float partial, boolean left) {
        float up = Mth.lerp(poseAmount(p, partial), -180f, -270f);
        float x = (float) Math.toRadians(up + 10f);
        float y = (float) Math.toRadians(left ? 5.0 : -5.0);
        float pitch = Mth.lerp(partial, p.xRotO, p.getXRot());
        float netYaw = Mth.rotLerp(partial, p.yHeadRotO, p.yHeadRot) - Mth.rotLerp(partial, p.yBodyRotO, p.yBodyRot);
        float yawOff = (float) Math.toRadians(Mth.wrapDegrees(netYaw) * -1f);
        float pitchOff = (float) Math.toRadians(pitch);
        return new float[]{x + pitchOff, y - yawOff, 0f};
    }

    public static boolean isLeftArm(Player p, InteractionHand renderHand) {
        boolean main = renderHand == InteractionHand.MAIN_HAND;
        return main ? p.getMainArm() == HumanoidArm.LEFT : p.getMainArm() == HumanoidArm.RIGHT;
    }

    /** Apply the flashlight pose on top of whatever (EMF / resource pack) already animated the arms. */
    public static void applyAfterEmf(Player p, ModelPart leftArm, ModelPart rightArm, ModelPart leftSleeve, ModelPart rightSleeve) {
        UUID id = p.getUUID();
        InteractionHand fh = Refl.flashlightHand(p);
        boolean on = fh != null && Refl.isOn(id) && !p.isSleeping();
        float partial = Minecraft.getInstance().getFrameTime();
        float blend = Refl.raisedBlend(id, partial); // already scaled down while ParCool is animating (ArmBlendMixin)
        if (!on && blend <= 0.0005f) return;
        InteractionHand rh = Refl.renderHand(id, fh);
        if (rh == null) return;
        boolean left = isLeftArm(p, rh);
        ModelPart arm = left ? leftArm : rightArm;
        ModelPart sleeve = left ? leftSleeve : rightSleeve;
        if (arm == null) return;
        float[] r = flashlightRot(p, partial, left);
        arm.xRot = Mth.lerp(blend, arm.xRot, r[0]);
        arm.yRot = Mth.lerp(blend, arm.yRot, r[1]);
        arm.zRot = Mth.lerp(blend, arm.zRot, r[2]);
        if (sleeve != null) {
            sleeve.xRot = arm.xRot;
            sleeve.yRot = arm.yRot;
            sleeve.zRot = arm.zRot;
        }
    }
}
