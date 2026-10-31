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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ChidoriItem extends Item {
    private static final double RANGE = 6.0;
    private static final float DAMAGE = 12.0F;
    private static final int COOLDOWN_TICKS = 40;

    public ChidoriItem(Properties properties) {
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
            Vec3 end = start.add(look.scale(RANGE));

            HitResult hit = level.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    serverPlayer
            ));

            Vec3 strikePoint = (hit.getType() == HitResult.Type.MISS) ? end : hit.getLocation();

            // Lightning trail papunta sa strike point.
            int steps = 10;
            for (int i = 1; i <= steps; i++) {
                Vec3 p = start.lerp(strikePoint, (double) i / steps);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
            }

            // Damage sa kahit sinong mob na nadaanan ng tusok.
            AABB sweepBox = new AABB(start, strikePoint).inflate(0.8);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    entity -> entity != serverPlayer && entity.isAlive());
            DamageSource damageSource = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, damageSource, DAMAGE);
            }

            // Basagin ang isang block na natamaan.
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                boolean broke = serverLevel.destroyBlock(blockHit.getBlockPos(), true, serverPlayer);
                if (broke) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            strikePoint.x, strikePoint.y, strikePoint.z, 15, 0.3, 0.3, 0.3, 0.1);
                }
            }

            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7F, 1.8F);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
