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

public class HanamaiBladeItem extends Item {
    private static final float DAMAGE = 5.0F;
    private static final double RADIUS = 3.5;
    private static final int COOLDOWN_TICKS = 30;

    public HanamaiBladeItem(Properties properties) {
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
            // Umiikot na petals sa paligid ng player, parang sumasayaw.
            Vec3 center = serverPlayer.position().add(0, 1.0, 0);
            int petals = 24;
            for (int i = 0; i < petals; i++) {
                double angle = (2 * Math.PI / petals) * i;
                double px = center.x + Math.cos(angle) * 1.3;
                double pz = center.z + Math.sin(angle) * 1.3;
                double py = center.y + Math.sin(angle * 2) * 0.4;
                serverLevel.sendParticles(ParticleTypes.CHERRY_LEAVES, px, py, pz, 1, 0, 0, 0, 0.0);
            }

            // Damage sa lahat ng mob sa paligid.
            AABB hitBox = serverPlayer.getBoundingBox().inflate(RADIUS);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox,
                    entity -> entity != serverPlayer && entity.isAlive());

            DamageSource damageSource = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, damageSource, DAMAGE);
                Vec3 knockback = target.position().subtract(serverPlayer.position()).normalize().scale(0.4);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.2, knockback.z));
            }

            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.3F);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
