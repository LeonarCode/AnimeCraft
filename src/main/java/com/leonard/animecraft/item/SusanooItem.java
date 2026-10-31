package com.leonard.animecraft.item;

import com.leonard.animecraft.entity.ModEntityTypes;
import com.leonard.animecraft.entity.SusanooArrowEntity;
import com.leonard.animecraft.entity.SusanooEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 3 behaviors ng isang right-click, depende sa kasalukuyang estado:
 * 1) Walang aktibong guardian         -> mag-summon.
 * 2) May aktibo, naka-SNEAK            -> i-dismiss.
 * 3) May aktibo, hindi naka-sneak      -> giant explosive arrow (tingnan ang SusanooArrowEntity).
 *
 * Ang sword-sweep (basic attack) ay hindi dito — nasa SusanooEffects.java iyon, dahil
 * global attack hook ito (kahit anong hawak mong weapon, susundan ka ng sweep habang active
 * ang guardian mo).
 */
public final class SusanooItem extends Item {
    private static final double LOOKUP_RADIUS = 8.0;
    private static final int SUMMON_COOLDOWN_TICKS = 20; // 1 segundo, pang-iwas sa di-sinasadyang double click

    public SusanooItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (!(serverPlayer.level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.FAIL;
        }

        SusanooEntity guardian = findGuardian(serverPlayer, serverLevel);

        if (guardian == null) {
            if (player.getCooldowns().isOnCooldown(stack)) {
                return InteractionResult.FAIL;
            }
            summon(serverPlayer, serverLevel);
            serverPlayer.getCooldowns().addCooldown(stack, SUMMON_COOLDOWN_TICKS);
            return InteractionResult.SUCCESS;
        }

        if (serverPlayer.isShiftKeyDown()) {
            dismiss(guardian, serverLevel);
            return InteractionResult.SUCCESS;
        }

        if (!guardian.canFireArrow()) {
            return InteractionResult.FAIL;
        }

        fireArrow(serverPlayer, serverLevel);
        guardian.triggerArrowCooldown();
        return InteractionResult.SUCCESS;
    }

    private SusanooEntity findGuardian(ServerPlayer player, ServerLevel level) {
        AABB nearby = player.getBoundingBox().inflate(LOOKUP_RADIUS);
        List<SusanooEntity> guardians = level.getEntitiesOfClass(SusanooEntity.class, nearby,
                g -> g.isAlive() && player.getUUID().equals(g.getOwnerUUID()));
        return guardians.isEmpty() ? null : guardians.get(0);
    }

    private void summon(ServerPlayer player, ServerLevel level) {
        SusanooEntity guardian = new SusanooEntity(ModEntityTypes.SUSANOO, level);
        guardian.setPos(player.getX(), player.getY(), player.getZ());
        guardian.setOwner(player);
        level.addFreshEntity(guardian);

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.5, 1.5, 0.5, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.7F);
    }

    private void dismiss(SusanooEntity guardian, ServerLevel level) {
        level.sendParticles(ParticleTypes.POOF,
                guardian.getX(), guardian.getY() + 1.0, guardian.getZ(), 30, 0.5, 1.0, 0.5, 0.05);
        level.playSound(null, guardian.getX(), guardian.getY(), guardian.getZ(),
                SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        guardian.discard();
    }

    private void fireArrow(ServerPlayer player, ServerLevel level) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 0.5F);

        ItemStack dummyStack = new ItemStack(ModItems.SUSANOO_WARD);
        Projectile.spawnProjectileFromRotation(SusanooArrowEntity::new, level, dummyStack,
                player, 0.0F, 2.2F, 0.5F);
    }
}