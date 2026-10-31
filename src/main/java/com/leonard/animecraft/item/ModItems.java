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
    public static final Item KUSANAGI_BLADE =
            register("kusanagi_blade", KusanagiBladeItem::new, new Item.Properties().stacksTo(1));
    public static final Item HANAMAI_BLADE =
            register("hanamai_blade", HanamaiBladeItem::new, new Item.Properties().stacksTo(1));
    public static final Item CRESCENT_REAVER =
            register("crescent_reaver", CrescentReaverItem::new, new Item.Properties().stacksTo(1));
    public static final Item FLAME_FANG =
            register("flame_fang", FlameFangItem::new, new Item.Properties().stacksTo(1));
    public static final Item RASENGAN =
            register("rasengan", RasenganItem::new, new Item.Properties().stacksTo(1));
    public static final Item CHIDORI =
            register("chidori", ChidoriItem::new, new Item.Properties().stacksTo(1));
    public static final Item CINDERCLAW_GAUNTLETS =
            register("cinderclaw_gauntlets", CinderclawGauntletsItem::new, new Item.Properties().stacksTo(1));
    public static final Item FROSTBIND_GAUNTLETS =
            register("frostbind_gauntlets", FrostbindGauntletsItem::new, new Item.Properties().stacksTo(1));
    public static final Item SHADOWFLAME_BRAND =
            register("shadowflame_brand", ShadowflameBrandItem::new, new Item.Properties().stacksTo(1));
    public static final Item KAMUI_SHARD =
            register("kamui_shard", KamuiItem::new, new Item.Properties().stacksTo(1));
    public static final Item SHADOW_CLONE_SEAL =
            register("shadow_clone_seal", ShadowCloneItem::new, new Item.Properties().stacksTo(1));
    public static final Item SUSANOO_WARD =
            register("susanoo_ward", SusanooItem::new, new Item.Properties().stacksTo(1));
    public static final Item SUSANOO_ARROW_HEAD = register("susanoo_arrow_head", Item::new, new Item.Properties());
    public static final Item MOON_GOD_DREAM =
            register("moon_god_dream", MoonGodDreamItem::new, new Item.Properties().stacksTo(1));
    public static final Item DIVINE_REPULSION =
            register("divine_repulsion", DivineRepulsionItem::new, new Item.Properties().stacksTo(1));
    public static final Item GRAVITY_TOMB =
            register("gravity_tomb", GravityTombItem::new, new Item.Properties().stacksTo(1));

    private ModItems() { }
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
                    tab.accept(KUSANAGI_BLADE);
                    tab.accept(HANAMAI_BLADE);
                    tab.accept(CRESCENT_REAVER);
                    tab.accept(FLAME_FANG);
                    tab.accept(RASENGAN);
                    tab.accept(CHIDORI);
                    tab.accept(CINDERCLAW_GAUNTLETS);
                    tab.accept(FROSTBIND_GAUNTLETS);
                    tab.accept(SHADOWFLAME_BRAND);
                    tab.accept(KAMUI_SHARD);
                    tab.accept(SHADOW_CLONE_SEAL);
                    tab.accept(SUSANOO_WARD);
                    tab.accept(MOON_GOD_DREAM);
                    tab.accept(DIVINE_REPULSION);
                    tab.accept(GRAVITY_TOMB);
                });
    }
}