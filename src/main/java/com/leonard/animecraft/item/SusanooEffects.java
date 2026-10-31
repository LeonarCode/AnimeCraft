package com.leonard.animecraft.item;

import com.leonard.animecraft.entity.SusanooEntity;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Dalawang bagay dito:
 * 1) Hinaharang ng Susanoo guardian ang damage na dapat sana tumama sa may-ari (ALLOW_DAMAGE).
 * 2) Sa bawat basic attack (left-click) ng may-ari habang active ang guardian, may dagdag na
 *    "sword sweep" na sumisira ng mga blocks sa unahan (AttackEntityCallback).
 *
 * PAALALA: kung hindi ma-resolve ang alinman sa "ServerLivingEntityEvents.ALLOW_DAMAGE" o
 * "AttackEntityCallback" sa bersyon mo, posibleng ibang pangalan na ito — Ctrl+N lang.
 */
public final class SusanooEffects {
    private static final double LOOKUP_RADIUS = 8.0;
    private static final int SWEEP_LENGTH = 4; // ilang blocks pasulong ang hiwa
    private static final double SWEEP_WIDTH = 2.5; // kalapad ng arc ng particles (kaliwa-kanan)

    private SusanooEffects() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(SusanooEffects::redirectDamage);
        AttackEntityCallback.EVENT.register(SusanooEffects::onAttack);
    }

    private static boolean redirectDamage(net.minecraft.world.entity.LivingEntity entity,
                                          net.minecraft.world.damagesource.DamageSource source,
                                          float amount) {
        if (!(entity instanceof ServerPlayer player)) {
            return true;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        SusanooEntity guardian = findGuardian(player, serverLevel);
        if (guardian == null) {
            return true;
        }

        guardian.hurtServer(serverLevel, source, amount);
        return false;
    }

    private static InteractionResult onAttack(Player player, Level level, InteractionHand hand,
                                              Entity target, EntityHitResult hitResult) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }

        SusanooEntity guardian = findGuardian(serverPlayer, serverLevel);
        if (guardian == null || !guardian.canSweep()) {
            return InteractionResult.PASS;
        }

        guardian.triggerSweepCooldown();
        performSweep(serverPlayer, serverLevel);
        return InteractionResult.PASS; // hindi natin hinaharang ang normal attack, dagdag lang ito
    }

    private static void performSweep(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x).normalize();

        for (int step = 1; step <= SWEEP_LENGTH; step++) {
            Vec3 center = eye.add(look.scale(step));
            BlockPos centerPos = BlockPos.containing(center);

            for (BlockPos pos : BlockPos.betweenClosed(
                    centerPos.offset(-1, -1, -1), centerPos.offset(1, 1, 1))) {
                BlockState state = level.getBlockState(pos);
                if (isSweepable(state, level, pos)) {
                    level.destroyBlock(pos, true);
                }
            }
        }

        // Malaking "sword slash" — buong arc ng particles mula kaliwa papuntang kanan sa
        // harap ng player, hindi lang iisang punto, para talagang mukhang hiniwa ng
        // malaking espada ang unahan (kasabay ng block destruction sa itaas).
        Vec3 swingCenter = eye.add(look.scale(2.0));
        for (double lateral = -SWEEP_WIDTH; lateral <= SWEEP_WIDTH; lateral += 0.5) {
            Vec3 point = swingCenter.add(right.scale(lateral));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CRIT, point.x, point.y, point.z, 2, 0.1, 0.1, 0.1, 0.02);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static boolean isSweepable(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        // Negatibo ang hardness ng mga "unbreakable" (hal. bedrock) — iyon lang ang ayaw natin.
        // PAALALA: kung masyadong madaling makasira ng mahahalagang bloke (ores, obsidian) gamit
        // ito, dito ka magdagdag ng mas mahigpit na hardness cap (hal. `hardness <= 2.5F`).
        return state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static SusanooEntity findGuardian(ServerPlayer player, ServerLevel level) {
        AABB nearby = player.getBoundingBox().inflate(LOOKUP_RADIUS);
        List<SusanooEntity> guardians = level.getEntitiesOfClass(SusanooEntity.class, nearby,
                guardian -> guardian.isAlive() && player.getUUID().equals(guardian.getOwnerUUID()));
        return guardians.isEmpty() ? null : guardians.get(0);
    }
}
