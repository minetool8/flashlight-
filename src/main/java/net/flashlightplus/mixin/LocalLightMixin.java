package net.flashlightplus.mixin;

import com.mojang.math.Axis;
import net.flashlightplus.compat.ParkourState;
import net.flashlightplus.compat.PoseMath;
import net.flashlightplus.compat.Refl;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;

/**
 * First person: the flashlight beam is normally locked to the camera. While ParCool is animating the
 * player, rotate the beam by how far the real arm has moved away from the normal hands-up pose.
 */
@Mixin(targets = "com.keerdm.flashlightmod.client.render.FlashlightLightDataManager", remap = false)
public abstract class LocalLightMixin {

    @Inject(method = "buildLocalPlayerLight", at = @At("RETURN"), cancellable = true, remap = false)
    private static void flashlightplus$followArm(Camera cam, LocalPlayer player, Matrix4f proj,
                                                 CallbackInfoReturnable<Object> cir) {
        try {
            Object r = cir.getReturnValue();
            if (r == null) return;
            float w = ParkourState.weight(player.getUUID());
            if (w < 0.001f) return;
            InteractionHand fh = Refl.flashlightHand(player);
            if (fh == null) return;
            boolean left = PoseMath.isLeftArm(player, fh);

            EntityRenderer<?> er = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (!(er instanceof LivingEntityRenderer<?, ?> lr) || !(lr.getModel() instanceof HumanoidModel<?> hm)) return;
            ModelPart arm = left ? hm.leftArm : hm.rightArm;

            float partial = Minecraft.getInstance().getFrameTime();
            float[] ref = PoseMath.flashlightRot(player, partial, left);
            float bodyYaw = net.minecraft.util.Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
            Vector3f wRef = toWorld(armDir(ref[0], ref[1], ref[2]), bodyYaw);
            Vector3f wCur = toWorld(armDir(arm.xRot, arm.yRot, arm.zRot), bodyYaw);

            Quaternionf q = new Quaternionf().rotationTo(wRef, wCur);
            q = new Quaternionf().slerp(q, w); // fade in/out with the parkour weight

            Class<?> c = r.getClass();
            Vec3 origin = (Vec3) c.getField("origin").get(r);
            Vec3 dir = (Vec3) c.getField("direction").get(r);
            Vector3f d = q.transform(new Vector3f((float) dir.x, (float) dir.y, (float) dir.z));
            Constructor<?> k = c.getConstructor(Vec3.class, Vec3.class, float.class, float.class, boolean.class, float.class);
            cir.setReturnValue(k.newInstance(origin, new Vec3(d.x, d.y, d.z),
                    c.getField("coneAngleCos").getFloat(r), c.getField("range").getFloat(r),
                    c.getField("isShadowed").getBoolean(r), c.getField("intensity").getFloat(r)));
        } catch (Throwable ignored) {
            // leave the original light untouched
        }
    }

    private static Vector3f armDir(float x, float y, float z) {
        return new Quaternionf().rotationZYX(z, y, x).transform(new Vector3f(0f, 1f, 0f));
    }

    /** model space -> world, mirroring LivingEntityRenderer (rotY(180-yaw), scale(-1,-1,1)). */
    private static Vector3f toWorld(Vector3f v, float bodyYaw) {
        Vector3f o = new Vector3f(-v.x, -v.y, v.z);
        return Axis.YP.rotationDegrees(180f - bodyYaw).transform(o);
    }
}
