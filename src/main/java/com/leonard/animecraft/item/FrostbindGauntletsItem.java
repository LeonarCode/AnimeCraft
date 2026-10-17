package com.leonard.animecraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class FrostbindGauntletsItem extends Item {
    private static final float DAMAGE = 4.0F;
    private static final double STRIKE_DISTANCE = 2.0;
    private static final double RADIUS = 3.5;
    private static final int SLOW_DURATION_TICKS = 100; // 5 segundo
    private static final int COOLDOWN_TICKS = 50;

    public FrostbindGauntletsItem(Properties properties) {
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
            Vec3 center = eye.add(look.scale(STRIKE_DISTANCE));

            AABB burstBox = new AABB(center, center).inflate(RADIUS);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, burstBox,
                    entity -> entity != serverPlayer && entity.isAlive());

            DamageSource damageSource = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, damageSource, DAMAGE);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_DURATION_TICKS, 2));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, SLOW_DURATION_TICKS, 1));
            }

            // Radial frost burst na particles.
            int points = 28;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double px = center.x + Math.cos(angle) * RADIUS;
                double pz = center.z + Math.sin(angle) * RADIUS;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, px, center.y, pz, 1, 0, 0.1, 0, 0.01);
            }
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 20, 0.4, 0.4, 0.4, 0.05);

            serverLevel.playSound(null, center.x, center.y, center.z,
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8F, 1.2F);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}