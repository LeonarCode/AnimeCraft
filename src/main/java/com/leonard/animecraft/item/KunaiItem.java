package com.leonard.animecraft.item;

import com.leonard.animecraft.entity.KunaiEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

public class KunaiItem extends Item implements ProjectileItem {
    public KunaiItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.6F, 1.0F);

        if (level instanceof ServerLevel serverLevel) {
            KunaiEntity kunai = Projectile.spawnProjectileFromRotation(KunaiEntity::new, serverLevel, itemStack,
                    player, 0.0F, 1.8F, 1.0F);

            // Itala kung saang kamay hawak ang kunai, para malaman natin kung alin ang
            // "kabilang kamay" pagdating/pag-teleport natin sa target.
            InteractionHand otherHand = (hand == InteractionHand.MAIN_HAND)
                    ? InteractionHand.OFF_HAND
                    : InteractionHand.MAIN_HAND;
            kunai.setArrivalHand(otherHand);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        itemStack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        return new KunaiEntity(level, position.x(), position.y(), position.z(), stack);
    }
}
