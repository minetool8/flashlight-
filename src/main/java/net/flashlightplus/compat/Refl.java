package net.flashlightplus.compat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** All access to Flashier Flashlights / TACZ / ParCool goes through reflection so nothing is needed at compile time. */
public final class Refl {
    private static boolean flInit, taczInit, parInit;
    private static Class<?> flashItemC;
    private static Method itemIsOn, itemToggle, handlerIsOn, handlerUpdate, blendRaised, blendRenderHand;
    private static Field channelF;
    private static Constructor<?> stateCtor;
    private static Method igunGet, igunGunId, apiIndex, indexType;
    private static Method parGet, parHasAnimator;

    private Refl() {}

    private static synchronized void initFl() {
        if (flInit) return;
        flInit = true;
        try {
            flashItemC = Class.forName("com.keerdm.flashlightmod.item.FlashlightItem");
            itemIsOn = flashItemC.getMethod("isFlashlightOn", ItemStack.class);
            itemToggle = flashItemC.getMethod("toggleFlashlight", ItemStack.class);
            Class<?> h = Class.forName("com.keerdm.flashlightmod.event.FlashlightToggleHandler");
            handlerIsOn = h.getMethod("isFlashlightOn", UUID.class);
            handlerUpdate = h.getMethod("updateClientFlashlightState", UUID.class, boolean.class);
            Class<?> nh = Class.forName("com.keerdm.flashlightmod.network.NetworkHandler");
            channelF = nh.getField("NETWORK_CHANNEL");
            Class<?> sp = Class.forName("com.keerdm.flashlightmod.network.packet.FlashlightStatePacket");
            stateCtor = sp.getConstructor(UUID.class, boolean.class);
        } catch (Throwable t) {
            System.err.println("[flashlightplus] core reflection failed: " + t);
        }
        try { // client-only class; fine if missing on a server
            Class<?> b = Class.forName("com.keerdm.flashlightmod.client.render.FlashlightArmBlendManager");
            blendRaised = b.getMethod("getRaisedBlend", UUID.class, float.class);
            blendRenderHand = b.getMethod("getRenderHand", UUID.class, InteractionHand.class);
        } catch (Throwable ignored) {}
    }

    private static synchronized void initTacz() {
        if (taczInit) return;
        taczInit = true;
        try {
            Class<?> ig = Class.forName("com.tacz.guns.api.item.IGun");
            igunGet = ig.getMethod("getIGunOrNull", ItemStack.class);
            igunGunId = ig.getMethod("getGunId", ItemStack.class);
            Class<?> api = Class.forName("com.tacz.guns.api.TimelessAPI");
            apiIndex = api.getMethod("getCommonGunIndex", net.minecraft.resources.ResourceLocation.class);
            Class<?> idx = Class.forName("com.tacz.guns.resource.index.CommonGunIndex");
            indexType = idx.getMethod("getType");
        } catch (Throwable ignored) { igunGet = null; }
    }

    private static synchronized void initPar() {
        if (parInit) return;
        parInit = true;
        try {
            Class<?> a = Class.forName("com.alrex.parcool.common.capability.Animation");
            parGet = a.getMethod("get", Player.class);
            parHasAnimator = a.getMethod("hasAnimator");
        } catch (Throwable ignored) { parGet = null; }
    }

    // ---------------- Flashlight ----------------
    public static boolean isFlashlight(ItemStack s) {
        initFl();
        return flashItemC != null && !s.isEmpty() && flashItemC.isInstance(s.getItem());
    }

    public static boolean isOn(UUID id) {
        initFl();
        try { return handlerIsOn != null && (Boolean) handlerIsOn.invoke(null, id); } catch (Throwable t) { return false; }
    }

    public static float raisedBlend(UUID id, float partial) {
        initFl();
        try { return blendRaised == null ? 0f : (Float) blendRaised.invoke(null, id, partial); } catch (Throwable t) { return 0f; }
    }

    public static InteractionHand renderHand(UUID id, InteractionHand h) {
        initFl();
        try { return blendRenderHand == null ? h : (InteractionHand) blendRenderHand.invoke(null, id, h); } catch (Throwable t) { return h; }
    }

    /** Same rule as the flashlight mod: main hand wins, otherwise off hand. */
    public static InteractionHand flashlightHand(Player p) {
        if (isFlashlight(p.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (isFlashlight(p.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    // ---------------- TACZ ----------------
    /** null = not a TACZ gun, otherwise lower-case type ("pistol", "rifle", "smg", ...; "?" if unknown). */
    public static String gunType(ItemStack s) {
        if (s.isEmpty()) return null;
        initTacz();
        if (igunGet == null) return null;
        try {
            Object ig = igunGet.invoke(null, s);
            if (ig == null) return null;
            Object id = igunGunId.invoke(ig, s);
            Object opt = apiIndex.invoke(null, id);
            if (opt instanceof Optional<?> o && o.isPresent()) {
                Object t = indexType.invoke(o.get());
                return t == null ? "?" : t.toString().toLowerCase(Locale.ROOT);
            }
            return "?";
        } catch (Throwable t) {
            return "?";
        }
    }

    /** Flashlight only in OFF hand + non-pistol gun in MAIN hand -> blocked. Flashlight in main hand is never blocked. */
    public static boolean offhandBlockedByBigGun(Player p) {
        ItemStack main = p.getMainHandItem();
        if (isFlashlight(main) || !isFlashlight(p.getOffhandItem())) return false;
        String t = gunType(main);
        return t != null && !t.equals("pistol");
    }

    public static void turnOffServer(ServerPlayer p) {
        initFl();
        try {
            ItemStack off = p.getOffhandItem();
            if ((Boolean) itemIsOn.invoke(null, off)) itemToggle.invoke(null, off);
            handlerUpdate.invoke(null, p.getUUID(), false);
            SimpleChannel ch = (SimpleChannel) channelF.get(null);
            ch.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), stateCtor.newInstance(p.getUUID(), false));
        } catch (Throwable t) {
            System.err.println("[flashlightplus] turnOff failed: " + t);
        }
    }

    // ---------------- ParCool ----------------
    public static boolean parcoolAnimating(Player p) {
        initPar();
        if (parGet == null) return false;
        try {
            Object a = parGet.invoke(null, p);
            return a != null && (Boolean) parHasAnimator.invoke(a);
        } catch (Throwable t) { return false; }
    }
}
