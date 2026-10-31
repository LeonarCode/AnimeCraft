package com.leonard.animecraft.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class RasenganItem extends Item {
    private static final DustParticleOptions CHAKRA_BLUE = new DustParticleOptions(0x55CCFF, 2.0F);
    private static final double STRIKE_DISTANCE = 2.5;

    // Maliit lang ang AOE (parang totoong power 3.0F na explosion) — block destruction, knockback,
    // crater shape. Ito yung "saklaw" ng Rasengan, hindi malayong-malayong blast.
    private static final float EXPLOSION_POWER = 3.0F;

    // Pero dagdag na DAMAGE (hindi radius) ito sa sinumang tamaan sa loob ng saklaw — para
    // pakiramdam nila parang 7.0F na pasabog ang tama, kahit maliit lang talaga ang sakop.
    private static final float BONUS_DAMAGE = 14.0F;

    private static final int COOLDOWN_TICKS = 60; // 3 segundo, mabigat na ability

    public RasenganItem(Properties properties) {
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
            Vec3 point = eye.add(look.scale(STRIKE_DISTANCE));

            // Chakra swirl bago sumabog.
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI / 20) * i;
                double px = point.x + Math.cos(angle) * 0.6;
                double pz = point.z + Math.sin(angle) * 0.6;
                serverLevel.sendParticles(CHAKRA_BLUE, px, point.y, pz, 1, 0, 0, 0, 0.0);
            }

            // Maliit na explosion lang — ito ang nagde-dedetermine ng AOE/saklaw (blocks, knockback).
            serverLevel.explode(serverPlayer, point.x, point.y, point.z,
                    EXPLOSION_POWER, Level.ExplosionInteraction.TNT);

            // Hiwalay na damage boost sa kahit sinong nasa loob ng maliit na saklaw — dito na
            // "pinalalakas" natin ang bagsak nito nang hindi kinakailangang palakihin ang radius.
            AABB damageArea = new AABB(point.x, point.y, point.z, point.x, point.y, point.z)
                    .inflate(EXPLOSION_POWER);
            List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, damageArea,
                    entity -> entity != serverPlayer && entity.isAlive());

            DamageSource rasenganDamage = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity victim : victims) {
                victim.hurtServer(serverLevel, rasenganDamage, BONUS_DAMAGE);
            }
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
