package com.leonard.animecraft.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Kamui Shard — hinihigop ng isang swirling dimension effect yung target na kalaban hanggang
 * mawala sila sa mundo (walang drop, walang death message, parang literal silang na-warp sa
 * ibang dimensyon). Mga mob lang ang tinatablan nito, hindi ibang manlalaro.
 */
public final class KamuiItem extends Item {
    private static final double RANGE = 8.0;
    private static final double TARGET_CONE_DOT = 0.95; // masikip na anggulo, parang tinutukan mo talaga siya
    private static final int COOLDOWN_TICKS = 150; // 7.5 segundo — malakas kasi, nawawala nang tuluyan ang kalaban

    public KamuiItem(Properties properties) {
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

        LivingEntity target = findTarget(serverPlayer);
        if (target == null) {
            return InteractionResult.FAIL;
        }

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.0F, 0.7F);

            KamuiEffects.warp(target, serverPlayer);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);

        return InteractionResult.SUCCESS;
    }

    private LivingEntity findTarget(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 reach = eyePos.add(look.scale(RANGE));

        HitResult blockHit = player.level().clip(new ClipContext(eyePos, reach,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistance = blockHit.getType() != HitResult.Type.MISS
                ? eyePos.distanceTo(blockHit.getLocation())
                : RANGE;

        LivingEntity closest = null;
        double closestDistance = maxDistance;

        for (Entity entity : player.level().getEntities(player, player.getBoundingBox().inflate(RANGE))) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            // Huwag galawin ang ibang manlalaro — mga mob lang ang tatablan ng Kamui na ito.
            if (living instanceof Player) {
                continue;
            }

            Vec3 toEntity = living.getBoundingBox().getCenter().subtract(eyePos);
            double distance = toEntity.length();
            if (distance > closestDistance || distance < 0.001) {
                continue;
            }

            double dot = toEntity.normalize().dot(look);
            if (dot < TARGET_CONE_DOT) {
                continue;
            }

            closest = living;
            closestDistance = distance;
        }

        return closest;
    }
}