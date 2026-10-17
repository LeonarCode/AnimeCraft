package com.leonard.animecraft.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class ShadowflameBrandItem extends Item {
    private static final DustParticleOptions DARK_FLAME = new DustParticleOptions(0x2B0033, 1.8F);
    private static final double RANGE = 10.0;
    private static final int BURN_DURATION_TICKS = 100; // 5 segundo, mas matagal + mas mataas ang damage kaysa normal apoy
    private static final int COOLDOWN_TICKS = 70;

    public ShadowflameBrandItem(Properties properties) {
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

            HitResult blockHit = level.clip(new ClipContext(
                    start, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    serverPlayer
            ));
            double maxDistance = (blockHit.getType() == HitResult.Type.MISS)
                    ? RANGE : start.distanceTo(blockHit.getLocation());

            AABB searchBox = new AABB(start, end).inflate(1.0);
            List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, searchBox,
                    entity -> entity != serverPlayer && entity.isAlive()
                            && start.distanceTo(entity.position()) <= maxDistance + 1.0);

            LivingEntity target = candidates.stream()
                    .min(Comparator.comparingDouble(e -> e.position().distanceToSqr(start)))
                    .orElse(null);

            if (target != null) {
                ShadowflameEffects.apply(target, serverPlayer, BURN_DURATION_TICKS);
                target.igniteForSeconds(BURN_DURATION_TICKS / 20);

                Vec3 targetPos = target.position().add(0, target.getBbHeight() * 0.5, 0);
                serverLevel.sendParticles(DARK_FLAME, targetPos.x, targetPos.y, targetPos.z,
                        30, 0.4, 0.5, 0.4, 0.05);
                serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                        SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0F, 0.6F);
            }

            // Trail ng dark particles papunta sa target (o sa dulo ng tingin kung walang tinamaan).
            Vec3 trailEnd = (target != null)
                    ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                    : end;
            int steps = 12;
            for (int i = 1; i <= steps; i++) {
                Vec3 p = start.lerp(trailEnd, (double) i / steps);
                serverLevel.sendParticles(DARK_FLAME, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}