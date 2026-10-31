package com.leonard.animecraft.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Susanoo — malaking "spirit guardian" na humaharang ng damage na dapat sana tumama sa
 * may-ari. Plain (hindi synced) field lang ang ownerUUID dito dahil server-side lang ito
 * binabasa (sa tick() para sa position-sync, at sa SusanooEffects para sa
 * damage-redirect lookup) — iba sa Shadow Clone na kailangan ng synced data dahil
 * binabasa ito ng CLIENT renderer para sa skin lookup. Fixed texture lang ang gamit ng
 * Susanoo renderer, walang per-owner data na kailangan sa client side.
 */
public class SusanooEntity extends PathfinderMob {
    private static final int MAX_DURATION_TICKS = 2400;
    private static final int SWEEP_COOLDOWN_TICKS = 15;
    private static final int ARROW_COOLDOWN_TICKS = 60;

    // Layo (sa likod ng player, base sa direksyon ng tingin) ng giant mula sa mismong
    // posisyon ng player — kung magkasabay, mapapasok ang camera sa loob mismo ng
    // scaled (4x) na model, kaya puro flat/dark na texture lang ang makikita.
    private static final double BEHIND_OFFSET = 2.5;

    private UUID ownerUUID;
    private int age;
    private int sweepCooldown;
    private int arrowCooldown;

    public SusanooEntity(EntityType<? extends SusanooEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public boolean canSweep() {
        return sweepCooldown <= 0;
    }

    public void triggerSweepCooldown() {
        sweepCooldown = SWEEP_COOLDOWN_TICKS;
    }

    public boolean canFireArrow() {
        return arrowCooldown <= 0;
    }

    public void triggerArrowCooldown() {
        arrowCooldown = ARROW_COOLDOWN_TICKS;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    public void setOwner(Player owner) {
        this.ownerUUID = owner.getUUID();
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    @Override
    protected void registerGoals() {
        // walang AI goals
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            return;
        }

        LivingEntity owner = null;
        if (ownerUUID != null && level() instanceof ServerLevel serverLevel) {
            owner = serverLevel.getPlayerByUUID(ownerUUID);
        }

        if (owner == null || !owner.isAlive()) {
            this.discard();
            return;
        }

        // Ilagay sa LIKOD ng player (base sa direksyon ng tingin), hindi eksaktong
        // kapareho ng posisyon niya — kung magkasabay, mapapasok ang camera ng player sa
        // loob mismo ng scaled (4x) na model, kaya "bulag" (puro flat/dark na texture).
        Vec3 look = owner.getLookAngle();
        double behindX = owner.getX() - look.x * BEHIND_OFFSET;
        double behindZ = owner.getZ() - look.z * BEHIND_OFFSET;
        this.setPos(behindX, owner.getY(), behindZ);

        this.setYRot(owner.getYRot());
        this.setXRot(0.0F);
        this.yBodyRot = owner.getYRot();
        this.yHeadRot = owner.getYRot();
        this.yRotO = owner.getYRot();

        if (sweepCooldown > 0) {
            sweepCooldown--;
        }
        if (arrowCooldown > 0) {
            arrowCooldown--;
        }

        age++;
        if (age >= MAX_DURATION_TICKS) {
            this.discard();
        }
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    getX(), getY() + getBbHeight() / 2.0, getZ(), 40, 0.5, 1.0, 0.5, 0.05);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.ENDER_DRAGON_HURT, SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        super.die(source);
    }
}
