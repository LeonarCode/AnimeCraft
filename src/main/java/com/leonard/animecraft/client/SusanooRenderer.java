package com.leonard.animecraft.client;

import com.leonard.animecraft.Animecraft;
import com.leonard.animecraft.entity.SusanooEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.Identifier;

/**
 * Pareho ang model dito sa Shadow Clone (HumanoidModel), pero PINALAKI natin sa pamamagitan ng
 * pag-override ng scale() — ganito rin ang ginagawa ng vanilla Giant/Enderman para lumaki ang
 * itsura nila kahit magkapareho lang ang mesh sa normal na humanoid.
 */
public class SusanooRenderer
        extends LivingEntityRenderer<SusanooEntity, SusanooRenderState, HumanoidModel<SusanooRenderState>> {

    private static final Identifier TEXTURE = Animecraft.id("textures/entity/susanoo.png");
    private static final float GIANT_SCALE = 4.0F;

    public SusanooRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModModelLayers.SUSANOO)), 2.0F);
    }

    @Override
    public SusanooRenderState createRenderState() {
        return new SusanooRenderState();
    }

    @Override
    protected void scale(SusanooRenderState state, PoseStack poseStack) {
        poseStack.scale(GIANT_SCALE, GIANT_SCALE, GIANT_SCALE);
    }

    @Override
    public Identifier getTextureLocation(SusanooRenderState state) {
        return TEXTURE;
    }
}