package com.leonard.animecraft.entity;

import com.leonard.animecraft.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class KunaiEntity extends ThrowableItemProjectile {
    public KunaiEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public KunaiEntity(Level level, LivingEntity owner, ItemStack itemStack) {
        super(ModEntityTypes.KUNAI, owner, level, itemStack);
    }

    public KunaiEntity(Level level, double x, double y, double z, ItemStack itemStack) {
        super(ModEntityTypes.KUNAI, x, y, z, level, itemStack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.TWIN_FANG_KUNAI;
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);

        if (level() instanceof ServerLevel serverLevel) {
            Vec3 pos = hitResult.getLocation();
            Entity owner = getOwner();

            if (owner != null) {
                owner.teleportTo(pos.x, pos.y, pos.z);
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        pos.x, pos.y, pos.z, 25, 0.3, 0.3, 0.3, 0.05);
                serverLevel.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            this.discard();
        }
    }
}
