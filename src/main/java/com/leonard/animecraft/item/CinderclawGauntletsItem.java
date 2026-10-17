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

public class CinderclawGauntletsItem extends Item {
    private static final float DAMAGE = 5.0F;
    private static final int FIRE_SECONDS = 5;
    private static final double RANGE = 4.0;
    private static final double ARC_DEGREES = 140.0;
    private static final int COOLDOWN_TICKS = 35;

    public CinderclawGauntletsItem(Properties properties) {
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
            Vec3 eye = serverPlayer.getEyePosition();
            Vec3 look = serverPlayer.getLookAngle();
            double cosThreshold = Math.cos(Math.toRadians(ARC_DEGREES / 2.0));

            AABB searchBox = serverPlayer.getBoundingBox().inflate(RANGE);
            List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                    entity -> entity != serverPlayer && entity.isAlive());

            DamageSource damageSource = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity target : nearby) {
                Vec3 toTarget = target.position().subtract(eye);
                double distance = toTarget.length();
                if (distance > RANGE || distance < 0.001) continue;

                if (look.dot(toTarget.normalize()) >= cosThreshold) {
                    target.hurtServer(serverLevel, damageSource, DAMAGE);
                    target.igniteForSeconds(FIRE_SECONDS);
                }
            }

            // Flame cone sweep papalabas sa harap.
            int arcPoints = 18;
            double halfArc = Math.toRadians(ARC_DEGREES / 2.0);
            for (int i = 0; i <= arcPoints; i++) {
                double t = -halfArc + (2 * halfArc) * i / arcPoints;
                Vec3 dir = rotateY(look, t);
                Vec3 point = eye.add(dir.scale(RANGE));
                serverLevel.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z, 2, 0.15, 0.15, 0.15, 0.02);
            }

            Vec3 punchPoint = eye.add(look.scale(1.5));
            serverLevel.sendParticles(ParticleTypes.FLAME, punchPoint.x, punchPoint.y, punchPoint.z, 12, 0.3, 0.3, 0.3, 0.04);

            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.1F);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    private static Vec3 rotateY(Vec3 vec, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        double x = vec.x * cos + vec.z * sin;
        double z = -vec.x * sin + vec.z * cos;
        return new Vec3(x, vec.y, z);
    }
}