package com.leonard.animecraft.ability;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AbilityRegistry {
    private static final Map<String, Ability> ABILITIES = new LinkedHashMap<>();

    private AbilityRegistry() {
    }

    public static Ability register(Ability ability) {
        if (ABILITIES.putIfAbsent(ability.id(), ability) != null) {
            throw new IllegalStateException("Duplicate ability id: " + ability.id());
        }
        return ability;
    }

    public static Optional<Ability> get(String id) {
        return Optional.ofNullable(ABILITIES.get(id));
    }

    public static Collection<Ability> all() {
        return Collections.unmodifiableCollection(ABILITIES.values());
    }

    public static void bootstrap() {
        if (!ABILITIES.isEmpty()) {
            return;
        }
        register(new Ability("shadow_clone", "Shadow Clone", PowerStyle.SHINOBI, 20, 100));
        register(new Ability("water_slash", "Water Slash", PowerStyle.BREATHING, 15, 60));
        register(new Ability("spirit_slash", "Spirit Slash", PowerStyle.REAPER, 25, 80));
        register(new Ability("fire_burst", "Fire Burst", PowerStyle.GUILD_MAGIC, 30, 120));
        register(new Ability("titan_form", "Titan Form", PowerStyle.TITAN, 80, 1200));
    }
}
