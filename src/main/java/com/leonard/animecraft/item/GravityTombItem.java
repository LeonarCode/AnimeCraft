package com.leonard.animecraft.item;

import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * "Gravity Tomb" — pangalawang Rinnegan-based na ability (konsepto: Chibaku Tensei /
 * "Planetary Devastation"). Hinahanap ang pinakamalapit na kalaban sa tinitingnan mo,
 * kinukuha ang lupa sa ilalim niya para gumawa ng malaking crater, tapos itatayo sa
 * ITAAS nito ang isang malaking globo ng stone na lumulutang — at ikukulong ang target
 * sa pinaka-gitna nito (literal na nakapaloob sa solid stone, walang puwang).
 *
 * Walang custom damage/tick logic na kailangan dito — dahil nakaumbok na siya sa loob
 * ng solid block, ang VANILLA suffocation mechanic mismo ("stuck in a block") ang
 * mag-asikaso ng "hanggang mamatay". Simpleng one-shot na ability lang ito, kagaya ng
 * DivineRepulsionItem — walang initialize() na kailangan idagdag sa Animecraft.java.
 */
public class GravityTombItem extends Item {
    private static final DustParticleOptions TOMB_PURPLE = new DustParticleOptions(0x8E24AA, 3.0F);
    private static final double RANGE = 12.0;
    private static final int SPHERE_RADIUS = 10; // "napakalaking group of blocks"
    private static final int CRATER_RADIUS = SPHERE_RADIUS; // kasing lawak ng sphere
    private static final int FLOAT_GAP = 4; // agwat ng crater sa lumulutang na globo
    private static final int COOLDOWN_TICKS = 300; // 15 segundo, pinakamalaking ultimate sa ngayon

    public GravityTombItem(Properties properties) {
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

        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

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

        if (target == null) {
            return InteractionResult.FAIL; // walang tinutukan, wag munang gamitin ang cooldown
        }

        BlockPos groundPos = target.blockPosition();

        carveCrater(serverLevel, groundPos);

        BlockPos sphereCenter = groundPos.above(SPHERE_RADIUS + FLOAT_GAP);
        buildSphere(serverLevel, sphereCenter);

        // I-ilusyon ng pagkabihag: direkta sa pinaka-gitna ng solid stone sphere.
        target.teleportTo(sphereCenter.getX() + 0.5, sphereCenter.getY(), sphereCenter.getZ() + 0.5);

        serverLevel.sendParticles(TOMB_PURPLE,
                sphereCenter.getX() + 0.5, sphereCenter.getY(), sphereCenter.getZ() + 0.5,
                200, SPHERE_RADIUS * 0.6, SPHERE_RADIUS * 0.6, SPHERE_RADIUS * 0.6, 0.1);

        serverLevel.playSound(null, groundPos.getX(), groundPos.getY(), groundPos.getZ(),
                SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 2.0F, 0.5F);

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    /** Kinukuha ang lupa sa ilalim ng target — bowl-shaped na crater, kasing lawak ng sphere. */
    private static void carveCrater(ServerLevel serverLevel, BlockPos groundPos) {
        for (int dx = -CRATER_RADIUS; dx <= CRATER_RADIUS; dx++) {
            for (int dz = -CRATER_RADIUS; dz <= CRATER_RADIUS; dz++) {
                double horizontal = Math.sqrt((double) (dx * dx + dz * dz));
                if (horizontal > CRATER_RADIUS) {
                    continue;
                }
                double depth = Math.sqrt((double) (CRATER_RADIUS * CRATER_RADIUS) - horizontal * horizontal);
                int depthBlocks = (int) Math.round(depth);
                for (int dy = 0; dy <= depthBlocks; dy++) {
                    serverLevel.setBlockAndUpdate(groundPos.offset(dx, -dy, dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    /** Lumulutang na globo ng stone sa itaas ng crater — dito makukulong ang target. */
    private static void buildSphere(ServerLevel serverLevel, BlockPos center) {
        int radiusSquared = SPHERE_RADIUS * SPHERE_RADIUS;
        for (int dx = -SPHERE_RADIUS; dx <= SPHERE_RADIUS; dx++) {
            for (int dy = -SPHERE_RADIUS; dy <= SPHERE_RADIUS; dy++) {
                for (int dz = -SPHERE_RADIUS; dz <= SPHERE_RADIUS; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                        serverLevel.setBlockAndUpdate(center.offset(dx, dy, dz), Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }
    }
}