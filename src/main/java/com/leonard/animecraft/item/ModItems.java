package com.leonard.animecraft.item;

import com.leonard.animecraft.Animecraft;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public final class ModItems {
    public static final Item TWIN_FANG_KUNAI =
            register("twin_fang_kunai", KunaiItem::new, new Item.Properties());

    public static final Item VERTICAL_GEAR =
            register("vertical_gear", VerticalGearItem::new, new Item.Properties());

    private ModItems() {
    }

    private static ResourceKey<Item> keyOf(String name) {
        return ResourceKey.create(Registries.ITEM, Animecraft.id(name));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties settings) {
        ResourceKey<Item> key = keyOf(name);
        Item item = factory.apply(settings.setId(key));
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
                .register(tab -> {
                    tab.accept(TWIN_FANG_KUNAI);
                    tab.accept(VERTICAL_GEAR);
                });
    }
}
