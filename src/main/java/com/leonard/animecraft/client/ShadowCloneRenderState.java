package com.leonard.animecraft.client;

import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Dinaragdagan natin ng sariling field ang karaniwang render state: kung anong skin texture
 * (galing sa may-ari ng clone) ang gagamitin sa pag-render nito. HumanoidRenderState (hindi
 * basta LivingEntityRenderState) dahil kailangan nito ng HumanoidModel.
 */
public class ShadowCloneRenderState extends HumanoidRenderState {
    public Identifier skinTexture;
}
