package com.leonard.animecraft.entity;

import com.leonard.animecraft.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class SusanooArrowEntity extends ThrowableItemProjectile {
    private static final float EXPLOSION_POWER = 3.0F;
    private static final float BONUS_DAMAGE = 18.0F;

    public SusanooArrowEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public SusanooArrowEntity(Level level, LivingEntity owner, ItemStack itemStack) {
        super(ModEntityTypes.SUSANOO_ARROW, owner, level, itemStack);
    }

    public SusanooArrowEntity(Level level, double x, double y, double z, ItemStack itemStack) {
        super(ModEntityTypes.SUSANOO_ARROW, x, y, z, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SUSANOO_WARD;
    }

    @Override
    public void tick() {
        super.tick();
        // Particle trail para talagang "giant/makapangyarihan" ang pakiramdam habang
        // lumilipad — default size lang kasi ang render ng thrown-item sprite, kaya dito
        // natin pinapalakas ang bisual sa halip na baguhin pa ang renderer (mapanganib
        // yan, baka ibang API pattern na naman ang gamit nito sa bersyon mo).
        if (!level().isClientSide() && level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    getX(), getY(), getZ(), 2, 0.15, 0.15, 0.15, 0.02);
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    getX(), getY(), getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (level() instanceof ServerLevel serverLevel) {
            double x = hitResult.getLocation().x, y = hitResult.getLocation().y, z = hitResult.getLocation().z;
            serverLevel.explode(getOwner(), x, y, z, EXPLOSION_POWER, Level.ExplosionInteraction.TNT);
            AABB damageArea = new AABB(x, y, z, x, y, z).inflate(EXPLOSION_POWER);
            List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, damageArea,
                    victim -> victim != getOwner() && victim.isAlive());
            DamageSource explosionDamage = serverLevel.damageSources().thrown(this, getOwner());
            for (LivingEntity victim : victims) {
                victim.hurtServer(serverLevel, explosionDamage, BONUS_DAMAGE);
            }
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0, 0, 0, 0);
            this.discard();
        }
    }
}
