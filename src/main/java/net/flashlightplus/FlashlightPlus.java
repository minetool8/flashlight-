package net.flashlightplus;

import net.flashlightplus.compat.ClientInit;
import net.flashlightplus.compat.Refl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("flashlightplus")
public class FlashlightPlus {
    public FlashlightPlus() {
        MinecraftForge.EVENT_BUS.register(this);
        if (FMLEnvironment.dist == Dist.CLIENT) ClientInit.init();
    }

    /** Server side: if a flashlight is on in the off hand while a big TACZ gun is in the main hand, switch it off. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer sp)) return;
        if (!Refl.isFlashlight(sp.getOffhandItem())) return;
        if (!Refl.isOn(sp.getUUID())) return;
        if (Refl.offhandBlockedByBigGun(sp)) Refl.turnOffServer(sp);
    }
}
