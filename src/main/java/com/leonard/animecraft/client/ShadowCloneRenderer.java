package com.leonard.animecraft.client;

import com.leonard.animecraft.entity.ShadowCloneEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

/**
 * Nire-render ang Shadow Clone gamit ang humanoid model, pero yung TEXTURE na ginagamit ay
 * kinukuha mula sa aktuwal na skin ng may-ari niya (kung online/malapit ito sa client).
 *
 * PINAKA-MADALAS MAGKA-ERROR DITO: yung getSkin()/texture lookup sa extractRenderState(), dahil
 * madalas nagbabago ang bahaging ito ng Minecraft sa mga bagong bersyon. Kung may error dito,
 * i-Ctrl+click yung getSkin() sa IDE para makita kung ano talaga ang tamang method/field name
 * ng PlayerSkin sa bersyon na ito.
 */
public class ShadowCloneRenderer
        extends LivingEntityRenderer<ShadowCloneEntity, ShadowCloneRenderState, HumanoidModel<ShadowCloneRenderState>> {

    private static final Identifier FALLBACK_SKIN =
            Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");

    public ShadowCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModModelLayers.SHADOW_CLONE)), 0.5F);
    }

    @Override
    public ShadowCloneRenderState createRenderState() {
        return new ShadowCloneRenderState();
    }

    @Override
    public void extractRenderState(ShadowCloneEntity clone, ShadowCloneRenderState state, float partialTick) {
        super.extractRenderState(clone, state, partialTick);

        state.skinTexture = FALLBACK_SKIN;

        if (clone.getOwnerUUID() != null
                && clone.level().getPlayerByUUID(clone.getOwnerUUID()) instanceof AbstractClientPlayer ownerPlayer) {
            state.skinTexture = ownerPlayer.getSkin().body().texturePath();
        }
    }

    @Override
    public Identifier getTextureLocation(ShadowCloneRenderState state) {
        return state.skinTexture;
    }
}