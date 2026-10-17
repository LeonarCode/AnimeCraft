package com.leonard.animecraft.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.UUID;

/** Sinusundan ng Shadow Clone kung sino man ang kahuli-hulihang sinalang ng may-ari niya. */
public class ShadowCloneMirrorTargetGoal extends Goal {
    private final ShadowCloneEntity clone;

    public ShadowCloneMirrorTargetGoal(ShadowCloneEntity clone) {
        this.clone = clone;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        LivingEntity ownerTarget = findOwnerTarget();
        return ownerTarget != null && ownerTarget.isAlive() && ownerTarget != clone;
    }

    @Override
    public void start() {
        clone.setTarget(findOwnerTarget());
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = clone.getTarget();
        return target != null && target.isAlive();
    }

    private LivingEntity findOwnerTarget() {
        UUID ownerUUID = clone.getOwnerUUID();
        if (ownerUUID == null) {
            return null;
        }

        Player owner = clone.level().getPlayerByUUID(ownerUUID);
        return owner != null ? owner.getLastHurtMob() : null;
    }
}