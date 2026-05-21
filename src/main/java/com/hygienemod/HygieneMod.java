package com.hygienemod;

import com.hygienemod.events.HygieneEventHandler;
import com.hygienemod.items.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HygieneMod.MOD_ID)
public class HygieneMod {
    public static final String MOD_ID = "hygiene";

    public HygieneMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new HygieneEventHandler());
    }
}
