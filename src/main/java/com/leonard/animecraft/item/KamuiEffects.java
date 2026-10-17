package com.leonard.animecraft.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KamuiEffects {
    private static final int WARP_DURATION_TICKS = 25; // ~1.25 segundong "paghigop" bago mawala
    private static final double START_RADIUS = 1.6;
    private static final int RING_POINTS = 10;
    private static final Map<UUID, Warp> ACTIVE = new ConcurrentHashMap<>();

    private KamuiEffects() {
    }

    public static void warp(LivingEntity target, ServerPlayer caster) {
        if (target instanceof Mob mob) {
            mob.setNoAi(true); // para hindi tumakbo habang hinihigop siya
        }
        target.setDeltaMovement(Vec3.ZERO);

        ACTIVE.put(target.getUUID(), new Warp(target, caster));
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Warp>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                if (!it.next().getValue().tick()) {
                    it.remove();
                }
            }
        });
    }

    private static final class Warp {
        private final LivingEntity target;
        private final ServerPlayer caster;
        private int ticksElapsed = 0;

        private Warp(LivingEntity target, ServerPlayer caster) {
            this.target = target;
            this.caster = caster;
        }

        private boolean tick() {
            if (!target.isAlive() || !(target.level() instanceof ServerLevel serverLevel)) {
                return false;
            }

            double progress = (double) ticksElapsed / WARP_DURATION_TICKS;
            double radius = START_RADIUS * (1.0 - progress);

            double cx = target.getX();
            double cy = target.getY() + target.getBbHeight() * 0.5;
            double cz = target.getZ();

            // Singsing ng portal particles na humihigpit papunta sa gitna — yung "dimension warp" visual.
            for (int i = 0; i < RING_POINTS; i++) {
                double angle = (2 * Math.PI / RING_POINTS) * i + progress * Math.PI * 4;
                double px = cx + Math.cos(angle) * radius;
                double pz = cz + Math.sin(angle) * radius;
                serverLevel.sendParticles(ParticleTypes.PORTAL, px, cy, pz, 1, 0.0, 0.05, 0.0, 0.0);
            }

            target.setDeltaMovement(Vec3.ZERO); // panatilihin sa puwesto habang hinihigop

            ticksElapsed++;
            if (ticksElapsed >= WARP_DURATION_TICKS) {
                serverLevel.playSound(null, cx, cy, cz, SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.PLAYERS, 1.0F, 0.6F);
                serverLevel.sendParticles(ParticleTypes.PORTAL, cx, cy, cz, 40, 0.3, 0.5, 0.3, 0.3);

                if (target instanceof Mob mob) {
                    mob.setNoAi(false);
                }
                target.discard(); // mawala sa mundo — walang drops, walang death message
                return false;
            }

            return true;
        }
    }
}
