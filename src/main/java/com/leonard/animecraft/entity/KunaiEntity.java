package com.leonard.animecraft.entity;

import com.leonard.animecraft.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class KunaiEntity extends ThrowableItemProjectile {
    private static final float DAMAGE = 6.0F; // 3 hearts bawat tama; 2 beses = double

    // Kabilang kamay (hindi yung ginamit para ihagis) — dito titingnan kung may "pang-arrival
    // attack" na ibang ability (hal. Rasengan) imbes na plain damage na lang.
    private InteractionHand arrivalHand = InteractionHand.OFF_HAND;

    public KunaiEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public KunaiEntity(Level level, LivingEntity owner, ItemStack itemStack) {
        super(ModEntityTypes.KUNAI, owner, level, itemStack);
    }

    public KunaiEntity(Level level, double x, double y, double z, ItemStack itemStack) {
        super(ModEntityTypes.KUNAI, x, y, z, level, itemStack);
    }

    public void setArrivalHand(InteractionHand hand) {
        this.arrivalHand = hand;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.TWIN_FANG_KUNAI;
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        // Bato ang tinamaan: walang damage, teleport lang.
        warpOwnerTo(hitResult.getLocation());
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);

        Entity hitEntity = hitResult.getEntity();
        Entity owner = getOwner();

        if (hitEntity instanceof LivingEntity target
                && hitEntity != owner
                && level() instanceof ServerLevel serverLevel) {

            // 1) Unang tama ng mismong kunai.
            DamageSource throwDamage = serverLevel.damageSources().thrown(this, owner);
            target.hurtServer(serverLevel, throwDamage, DAMAGE);

            warpOwnerTo(hitResult.getLocation());

            // 2) Bonus "arrival strike" — kung buhay pa ang target pagdating mo.
            if (target.isAlive()) {
                if (owner instanceof ServerPlayer player) {
                    ItemStack offHandStack = player.getItemInHand(arrivalHand);

                    if (!offHandStack.isEmpty()) {
                        // May hawak sa kabilang kamay (hal. Rasengan) — ang ability niyan mismo
                        // ang gagamitin bilang arrival attack, kasama lahat ng sarili niyang
                        // particles/damage/cooldown. Mag-ra-raycast ito mula sa bagong puwesto
                        // mo dahil nakateleport ka na.
                        offHandStack.getItem().use(serverLevel, player, arrivalHand);
                    } else {
                        // Walang hawak sa kabilang kamay — normal na bonus damage na lang.
                        DamageSource arrivalDamage = serverLevel.damageSources().playerAttack(player);
                        target.hurtServer(serverLevel, arrivalDamage, DAMAGE);
                    }
                } else if (owner instanceof LivingEntity livingOwner) {
                    DamageSource arrivalDamage = serverLevel.damageSources().mobAttack(livingOwner);
                    target.hurtServer(serverLevel, arrivalDamage, DAMAGE);
                }
            }
        } else {
            warpOwnerTo(hitResult.getLocation());
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!level().isClientSide()) {
            this.discard();
        }
    }

    private void warpOwnerTo(Vec3 pos) {
        if (level() instanceof ServerLevel serverLevel) {
            Entity owner = getOwner();
            if (owner != null) {
                owner.teleportTo(pos.x, pos.y, pos.z);
                serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        pos.x, pos.y, pos.z, 25, 0.3, 0.3, 0.3, 0.05);
                serverLevel.playSound(null, pos.x, pos.y, pos.z,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }
}
