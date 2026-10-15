package com.leonard.animecraft.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GrappleEffects {
    private static final Map<UUID, Grapple> ACTIVE = new ConcurrentHashMap<>();

    private GrappleEffects() {
    }

    public static void start(ServerPlayer player, Vec3 target) {
        ACTIVE.put(player.getUUID(), new Grapple(player, target, 30));
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Grapple>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                if (!it.next().getValue().tick()) {
                    it.remove();
                }
            }
        });
    }

    private static final class Grapple {
        private final ServerPlayer player;
        private final Vec3 target;
        private int ticksLeft;

        private Grapple(ServerPlayer player, Vec3 target, int ticksLeft) {
            this.player = player;
            this.target = target;
            this.ticksLeft = ticksLeft;
        }

        private boolean tick() {
            if (player.isRemoved() || ticksLeft-- <= 0) {
                return false;
            }

            Vec3 from = player.getEyePosition();
            Vec3 toTarget = target.subtract(from);
            double distance = toTarget.length();

            if (distance < 1.5) {
                return false;
            }

            Vec3 pull = toTarget.normalize().scale(Math.min(2.4, distance * 0.3 + 0.6));
            player.setDeltaMovement(pull);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
            player.resetFallDistance();

            if (player.level() instanceof ServerLevel serverLevel) {
                int segments = (int) Math.max(2, distance * 2);
                for (int i = 1; i <= segments; i++) {
                    Vec3 point = from.lerp(target, (double) i / segments);
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            point.x, point.y, point.z, 1, 0, 0, 0, 0.0);
                }
            }

            return true;
        }
    }
}
