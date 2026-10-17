package com.leonard.animecraft.client;

import com.leonard.animecraft.Animecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

public final class ModModelLayers {
    public static final ModelLayerLocation SHADOW_CLONE =
            new ModelLayerLocation(Animecraft.id("shadow_clone"), "main");

    private ModModelLayers() {
    }

    public static LayerDefinition createShadowCloneLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(mesh, 64, 64);
    }
}
