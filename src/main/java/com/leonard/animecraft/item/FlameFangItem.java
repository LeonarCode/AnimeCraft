package com.leonard.animecraft.item;

import net.minecraft.core.particles.ParticleTypes;
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

public class FlameFangItem extends Item {
    private static final float DAMAGE = 6.0F;
    private static final int FIRE_SECONDS = 4;
    private static final double RANGE = 4.5;
    private static final double WIDTH = 2.0;
    private static final int COOLDOWN_TICKS = 45;

    public FlameFangItem(Properties properties) {
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
            Vec3 center = serverPlayer.position().add(0, 1.0, 0);

            // Damage + sunog sa lahat ng mob sa harapan.
            AABB fireBox = new AABB(start, start.add(look.scale(RANGE))).inflate(WIDTH);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, fireBox,
                    entity -> entity != serverPlayer && entity.isAlive());

            DamageSource damageSource = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, damageSource, DAMAGE);
                target.igniteForSeconds(FIRE_SECONDS);
            }

            // Flame arc sa harapan — parang paghinga ng apoy.
            double baseAngle = Math.atan2(look.z, look.x);
            double arcSpan = Math.toRadians(120);
            int points = 16;
            for (int i = 0; i < points; i++) {
                double t = (double) i / (points - 1);
                double angle = baseAngle - arcSpan / 2 + arcSpan * t;
                double radius = 1.5 + (i % 3) * 0.5;
                double px = center.x + Math.cos(angle) * radius;
                double pz = center.z + Math.sin(angle) * radius;
                serverLevel.sendParticles(ParticleTypes.FLAME, px, center.y, pz, 1, 0, 0.1, 0, 0.01);
            }

            // Burst sa gitna, parang ignition.
            Vec3 burstPoint = center.add(look.scale(1.0));
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    burstPoint.x, burstPoint.y, burstPoint.z, 15, 0.4, 0.4, 0.4, 0.02);

            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.1F);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
