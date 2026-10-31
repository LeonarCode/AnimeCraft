package com.leonard.animecraft;

import com.leonard.animecraft.ability.AbilityRegistry;
import com.leonard.animecraft.ability.PowerStyle;
import com.leonard.animecraft.entity.ModEntityTypes;
import com.leonard.animecraft.entity.ShadowCloneEntity;
import com.leonard.animecraft.item.*;
import com.leonard.animecraft.entity.SusanooEntity;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public class Animecraft implements ModInitializer {
    public static final String MOD_ID = "animecraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        AbilityRegistry.bootstrap();
        ModItems.initialize();
        ModEntityTypes.initialize();
        GrappleEffects.initialize();
        ShadowflameEffects.initialize();
        KamuiEffects.initialize();
        SusanooEffects.initialize();
        MoonGodDreamEffects.initialize();

        FabricDefaultAttributeRegistry.register(ModEntityTypes.SHADOW_CLONE, ShadowCloneEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ModEntityTypes.SUSANOO, SusanooEntity.createAttributes());

        LOGGER.info("Animecraft loaded with {} abilities across {} styles.",
                AbilityRegistry.all().size(), PowerStyle.values().length);
    }
}
