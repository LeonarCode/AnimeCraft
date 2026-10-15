package com.leonard.animecraft;

import com.leonard.animecraft.ability.AbilityRegistry;
import com.leonard.animecraft.ability.PowerStyle;
import com.leonard.animecraft.entity.ModEntityTypes;
import com.leonard.animecraft.item.GrappleEffects;
import com.leonard.animecraft.item.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        LOGGER.info("Animecraft loaded with {} abilities across {} styles.",
                AbilityRegistry.all().size(), PowerStyle.values().length);
    }
}
