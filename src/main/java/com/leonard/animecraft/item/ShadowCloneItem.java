package com.leonard.animecraft.item;

import com.leonard.animecraft.entity.ModEntityTypes;
import com.leonard.animecraft.entity.ShadowCloneEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ShadowCloneItem extends Item {
    private static final int COOLDOWN_TICKS = 300; // 15 segundo

    public ShadowCloneItem(Properties properties) {
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

        Vec3 spawnPos = player.position().subtract(player.getLookAngle().normalize().scale(1.5));

        ShadowCloneEntity clone = new ShadowCloneEntity(ModEntityTypes.SHADOW_CLONE, serverLevel);
        clone.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        clone.setYRot(player.getYRot());
        clone.setXRot(0.0F);
        clone.setOwner(serverPlayer);
        serverLevel.addFreshEntity(clone);

        serverLevel.sendParticles(ParticleTypes.POOF, spawnPos.x, spawnPos.y + 1.0, spawnPos.z,
                20, 0.3, 0.5, 0.3, 0.02);
        serverLevel.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0F, 1.3F);

        serverPlayer.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }
}
