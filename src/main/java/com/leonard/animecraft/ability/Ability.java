package com.leonard.animecraft.ability;

public record Ability(
        String id,
        String displayName,
        PowerStyle style,
        int energyCost,
        int cooldownTicks
) {
    public Ability {
        if (energyCost < 0 || cooldownTicks < 0) {
            throw new IllegalArgumentException("energyCost and cooldownTicks must be >= 0");
        }
    }
}
