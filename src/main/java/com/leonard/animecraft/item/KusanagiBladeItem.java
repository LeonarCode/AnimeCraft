package com.leonard.animecraft.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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

public class KusanagiBladeItem extends Item {
    private static final double DASH_SPEED = 1.6;
    private static final double STRIKE_RANGE = 5.0;
    private static final double STRIKE_WIDTH = 1.2;
    private static final float DAMAGE = 12.0F; // 5 hearts — malakas dahil de-kidlat
    private static final int COOLDOWN_TICKS = 40; // 2 segundo

    public KusanagiBladeItem(Properties properties) {
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

        Vec3 look = serverPlayer.getLookAngle();
        Vec3 dash = look.scale(DASH_SPEED);

        serverPlayer.setDeltaMovement(dash);
        serverPlayer.resetFallDistance();
        serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));

        if (serverPlayer.level() instanceof ServerLevel serverLevel) {
            Vec3 start = serverPlayer.position();

            // Lightning trail habang dumadash.
            for (int i = 1; i <= 8; i++) {
                Vec3 point = start.add(dash.scale(i * 0.4));
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y + 1.0, point.z, 2, 0.15, 0.15, 0.15, 0.0);
            }

            // Malakas na tama sa kahit sinong mob na nasa dinadaanan ng dash.
            Vec3 strikeEnd = start.add(look.scale(STRIKE_RANGE));
            AABB strikeBox = new AABB(start, strikeEnd).inflate(STRIKE_WIDTH);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, strikeBox,
                    entity -> entity != serverPlayer && entity.isAlive());

            DamageSource damageSource = serverLevel.damageSources().playerAttack(serverPlayer);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, damageSource, DAMAGE);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        10, 0.3, 0.3, 0.3, 0.05);
            }

            serverLevel.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.6F, 1.6F);
        }

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
