package com.leonard.animecraft.entity;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

import java.util.Optional;
import java.util.UUID;

/**
 * Shadow Clone — binubuhay ng manlalaro bilang kasama sa laban. Sinusundan nito kung sino man
 * ang kahuli-hulihang sinalang ng may-ari (tingnan ang {@link ShadowCloneMirrorTargetGoal}), at
 * mawawala na lang pagkatapos ng {@link #LIFESPAN_TICKS}.
 *
 * Ang ownerUUID ay SYNCED ENTITY DATA (hindi plain field) dahil kailangan itong makita ng
 * CLIENT SIDE para malaman kung kaninong skin ang gagamitin sa pag-render (ShadowCloneRenderer).
 * Kung plain field lang ito, server-side info lang siya at laging mananatiling walang laman sa
 * client, kaya laging Steve/fallback ang lumalabas.
 */
public class ShadowCloneEntity extends PathfinderMob {
    private static final int LIFESPAN_TICKS = 600; // 30 segundo bago mawala ang clone
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> DATA_OWNER_REF =
            SynchedEntityData.defineId(ShadowCloneEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    private int age;

    public ShadowCloneEntity(EntityType<? extends ShadowCloneEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_REF, Optional.empty());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public void setOwner(Player owner) {
        EntityReference<LivingEntity> ref = EntityReference.of(owner);
        this.entityData.set(DATA_OWNER_REF, Optional.of(ref));
    }

    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_REF).map(EntityReference::getUUID).orElse(null);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new ShadowCloneMirrorTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide()) {
            age++;
            if (age >= LIFESPAN_TICKS) {
                this.discard();
            }
        }
    }
}
