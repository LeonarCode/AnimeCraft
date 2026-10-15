package com.leonard.animecraft.client;

import com.leonard.animecraft.entity.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class AnimecraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModEntityTypes.KUNAI, ThrownItemRenderer::new);
    }
}
