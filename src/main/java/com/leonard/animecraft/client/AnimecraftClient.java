package com.leonard.animecraft.client;

import com.leonard.animecraft.entity.ModEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class AnimecraftClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRenderers.register(ModEntityTypes.KUNAI, ThrownItemRenderer::new);

        ModelLayerRegistry.registerModelLayer(ModModelLayers.SHADOW_CLONE, ModModelLayers::createShadowCloneLayer);
        EntityRenderers.register(ModEntityTypes.SHADOW_CLONE, ShadowCloneRenderer::new);
    }
}
