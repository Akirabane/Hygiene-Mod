package com.hygienemod.items;

import com.hygienemod.HygieneMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HygieneMod.MOD_ID);

    public static final RegistryObject<Item> VIKING_SOAP =
            ITEMS.register("viking_soap", VikingSoapItem::new);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
