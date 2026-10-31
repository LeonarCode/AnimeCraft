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

    public static final ResourceKey<EntityType<?>> SHADOW_CLONE_ID = create("shadow_clone");

    public static final EntityType<ShadowCloneEntity> SHADOW_CLONE = register(
            SHADOW_CLONE_ID,
            EntityType.Builder.<ShadowCloneEntity>of(ShadowCloneEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f) // kasing-laki ng manlalaro
                    .clientTrackingRange(10)
                    .updateInterval(3)
    );

    public static final ResourceKey<EntityType<?>> SUSANOO_ID = create("susanoo");

    public static final EntityType<SusanooEntity> SUSANOO = register(
            SUSANOO_ID,
            EntityType.Builder.<SusanooEntity>of(SusanooEntity::new, MobCategory.MISC)
                    .sized(0.8f, 2.5f) // hitbox lang ito — yung VISUAL na kalakihan ay sa renderer (scale)
                    .clientTrackingRange(16)
                    .updateInterval(3)
                    .fireImmune()
    );

    public static final ResourceKey<EntityType<?>> SUSANOO_ARROW_ID = create("susanoo_arrow");

    public static final EntityType<SusanooArrowEntity> SUSANOO_ARROW = register(
            SUSANOO_ARROW_ID,
            EntityType.Builder.<SusanooArrowEntity>of(SusanooArrowEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f)
                    .clientTrackingRange(8)
                    .updateInterval(5)
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
