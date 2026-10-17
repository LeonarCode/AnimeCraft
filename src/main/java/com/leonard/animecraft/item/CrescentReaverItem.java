package com.leonard.animecraft.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;


import java.util.List;

public class CrescentReaverItem extends Item {
    private static final float DAMAGE = 7.0F;
    private static final double RANGE = 12.0;
    private static final double WIDTH = 1.5;
    private static final int COOLDOWN_TICKS = 50;
    private static final DustParticleOptions RED_WAVE =
            new DustParticleOptions(0xFF1919, 1.8F);

    public CrescentReaverItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            Vec3 start = serverPlayer.getEyePosition();
            Vec3 look = serverPlayer.getLookAngle();

            AABB sweepBox = new AABB(start, start.add(look.scale(RANGE))).inflate(WIDTH);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    entity -> entity != serverPlayer && entity.isAlive());

            DamageSource damageSource = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, damageSource, DAMAGE);
                Vec3 push = look.scale(0.6);
                target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.15, push.z));

                serverLevel.sendParticles(RED_WAVE,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        12, 0.3, 0.3, 0.3, 0.05);
            }

            int steps = 8;
            for (int i = 1; i <= steps; i++) {
                Vec3 point = start.add(look.scale(i * (RANGE / steps)));
                serverLevel.sendParticles(RED_WAVE, point.x, point.y, point.z, 3, 0.15, 0.15, 0.15, 0.0);
            }

            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5F, 1.4F);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
