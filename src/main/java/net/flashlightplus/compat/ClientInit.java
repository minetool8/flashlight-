package net.flashlightplus.compat;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientInit {
    private ClientInit() {}

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener((FMLClientSetupEvent e) -> {
            if (ModList.get().isLoaded("entity_model_features")) {
                e.enqueueWork(() -> {
                    try { EmfCompat.register(); }
                    catch (Throwable t) { System.err.println("[flashlightplus] EMF hook failed: " + t); }
                });
            }
        });
    }
}
