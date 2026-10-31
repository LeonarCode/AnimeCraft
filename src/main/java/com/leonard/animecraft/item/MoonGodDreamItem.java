package com.leonard.animecraft.item;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * "Moon God Dream" (Tsukuyomi) — kinukuha ang pinakamalapit na kalaban sa tinitingnan mo,
 * pareho kayong "hinihila" papunta sa Nether (ginagamit bilang "ibang mundo" ng panaginip).
 * Tingnan ang MoonGodDreamEffects.java para sa buong mekanismo (buffs/debuffs, immunity,
 * auto-return).
 */
public class MoonGodDreamItem extends Item {
    private static final DustParticleOptions MOON_PURPLE = new DustParticleOptions(0x3A1A5C, 2.2F);
    private static final double RANGE = 10.0;
    private static final int COOLDOWN_TICKS = 200; // 10 segundo, ultimate-tier na ability

    public MoonGodDreamItem(Properties properties) {
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

        if (serverPlayer.level().dimension() != Level.OVERWORLD) {
            // Dapat nasa Overworld tayo bago gamitin ito, dahil doon natin pareho ibabalik
            // ang caster at target pagkatapos.
            return InteractionResult.FAIL;
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

        MoonGodDreamEffects.begin(serverPlayer, target);

        serverLevel.sendParticles(MOON_PURPLE, target.getX(), target.getY() + target.getBbHeight() * 0.5,
                target.getZ(), 40, 0.4, 0.5, 0.4, 0.05);

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
