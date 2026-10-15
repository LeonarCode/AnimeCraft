package com.leonard.animecraft.entity;

import com.leonard.animecraft.Animecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntityTypes {
    public static final ResourceKey<EntityType<?>> KUNAI_ID = create("kunai");

    public static final EntityType<KunaiEntity> KUNAI = register(
            KUNAI_ID,
            EntityType.Builder.<KunaiEntity>of(KunaiEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(4)
                    .updateInterval(10)
    );

    private ModEntityTypes() {
    }

    private static ResourceKey<EntityType<?>> create(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Animecraft.id(name));
    }

    private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType.Builder<T> builder) {
        EntityType<T> type = builder.build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
    }

    public static void initialize() {
        // Pag-reference lang sa class na ito, nagre-run na ang static fields sa itaas.
    }
}
