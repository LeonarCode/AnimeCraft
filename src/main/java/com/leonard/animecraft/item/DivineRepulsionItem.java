package com.leonard.animecraft.item;

import net.minecraft.core.particles.DustParticleOptions;
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

/**
 * "Divine Repulsion" — unang Rinnegan-based na ability ("Almighty Push" sa konsepto).
 * Omnidirectional na pagsabog MULA sa mismong puwesto ng caster — hindi tulad ng ibang
 * item na tumutok sa unahan, ito ay sumasabog PALIBOT sa caster, kaya siya mismo ay
 * hindi apektado (parehong pag-exclude sa caster na ginagawa natin sa RasenganItem).
 *
 * Katamtaman lang ang EXPLOSION_POWER (para sa AOE reach/block destruction, hindi
 * sobrang laki na terrain damage) — ang "10x TNT" na pakiramdam ay galing sa HIWALAY
 * na BONUS_DAMAGE at KNOCKBACK_STRENGTH na dinadagdag natin mismo (parehong pattern na
 * ginamit sa RasenganItem at SusanooArrowEntity: hiwalay ang radius/blocks sa
 * damage/knockback magnitude).
 */
public class DivineRepulsionItem extends Item {
    private static final DustParticleOptions SPIRAL_PURPLE = new DustParticleOptions(0x8E24AA, 3.0F);
    private static final float EXPLOSION_POWER = 8.0F; // AOE reach/block destruction (2x TNT)
    private static final double DAMAGE_RADIUS = 12.0; // saklaw ng bonus damage/knockback
    private static final float BONUS_DAMAGE = 40.0F; // "10x TNT" na pakiramdam
    private static final double KNOCKBACK_STRENGTH = 3.5; // sobrang lakas na pagtulak palayo
    private static final int COOLDOWN_TICKS = 160; // 8 segundo, ultimate-tier na ability

    public DivineRepulsionItem(Properties properties) {
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

        // Sa gitna ng dibdib ng caster ang sentro ng "push" — siya ang pinanggagalingan.
        Vec3 center = serverPlayer.position().add(0, serverPlayer.getBbHeight() * 0.5, 0);

        spawnRipple(serverLevel, center);

        // Katamtaman lang ang explosion power — para sa AOE reach/pagsira ng blocks,
        // hindi para literal na 10x na TNT crater (iwas labis na world destruction/lag).
        serverLevel.explode(serverPlayer, center.x, center.y, center.z,
                EXPLOSION_POWER, Level.ExplosionInteraction.TNT);

        AABB damageArea = new AABB(center, center).inflate(DAMAGE_RADIUS);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, damageArea,
                entity -> entity != serverPlayer && entity.isAlive());

        DamageSource pushDamage = serverLevel.damageSources().playerAttack(serverPlayer);

        for (LivingEntity victim : victims) {
            victim.hurtServer(serverLevel, pushDamage, BONUS_DAMAGE);

            Vec3 away = victim.position().subtract(center);
            double distance = Math.max(away.length(), 0.5);
            Vec3 direction = away.scale(1.0 / distance);
            // Mas malapit sa sentro, mas malakas ang itulak (falloff papalayo).
            double falloff = Math.max(0.15, 1.0 - (distance / DAMAGE_RADIUS));
            double strength = KNOCKBACK_STRENGTH * falloff;

            Vec3 push = new Vec3(
                    direction.x * strength,
                    Math.max(direction.y, 0.4) * strength,
                    direction.z * strength
            );
            victim.setDeltaMovement(victim.getDeltaMovement().add(push));
        }

        serverLevel.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.6F);

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    /** Spiral/ripple na particle rings na sumasabog palabas mula sa caster bago ang blast. */
    private static void spawnRipple(ServerLevel serverLevel, Vec3 center) {
        for (int ring = 1; ring <= 4; ring++) {
            double radius = ring * 1.2;
            int points = 16 * ring;
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double px = center.x + Math.cos(angle) * radius;
                double pz = center.z + Math.sin(angle) * radius;
                serverLevel.sendParticles(SPIRAL_PURPLE, px, center.y, pz, 1, 0, 0, 0, 0.0);
            }
        }
    }
}
