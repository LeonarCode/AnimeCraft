package com.leonard.animecraft.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "Moon God Dream" (Tsukuyomi) — hinihila ng caster ang kalaban papasok sa sarili niyang
 * mundo ng panaginip. Sa totoong Minecraft, ginagamit natin ang Nether bilang ang "ibang
 * mundo": PAREHO silang teleport doon (sa isang nakalaang platform), pagkatapos:
 *  - Caster: buffed (Night Vision, Fire Resistance, Strength) — siya ang may kontrol.
 *  - Target: debuffed (Blindness, Confusion, Slowness) — nalilito/nabibigla.
 *  - Pareho silang IMMUNE sa panlabas na panganib (ibang mobs, Nether hazards) habang
 *    nandoon — PERO hindi immune sa isa't isa, kaya puwede pa ring saktan ng caster ang
 *    target gamit ang bagong Strength niya.
 *  - Pagkatapos ng DURATION_TICKS, PAREHO silang babalik sa eksaktong dating puwesto nila.
 */
public final class MoonGodDreamEffects {
    private static final BlockPos PLATFORM_CENTER = new BlockPos(30000, 100, 30000);
    private static final int DURATION_TICKS = 140; // ~7 segundo

    private static final Map<UUID, Duel> ACTIVE = new ConcurrentHashMap<>();

    private MoonGodDreamEffects() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Duel>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Duel> entry = it.next();
                // Isang Duel object lang ang nagta-track sa dalawang UUID (caster + target),
                // kaya isang beses lang natin dapat i-tick ito bawat server tick — ang caster
                // UUID na lang ang "may-ari" ng pag-tick, yung target UUID entry ay tumuturo
                // lang sa parehong Duel object para sa mabilis na lookup sa allowDamage().
                if (entry.getKey().equals(entry.getValue().casterUUID) && !entry.getValue().tick()) {
                    ACTIVE.remove(entry.getValue().casterUUID);
                    ACTIVE.remove(entry.getValue().targetUUID);
                }
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(MoonGodDreamEffects::allowDamage);
    }

    public static void begin(ServerPlayer caster, LivingEntity target) {
        if (ACTIVE.containsKey(caster.getUUID()) || ACTIVE.containsKey(target.getUUID())) {
            return; // may aktibo nang duel na kasali ang alinman sa dalawa
        }

        MinecraftServer server = caster.level().getServer();
        if (server == null) {
            return;
        }

        ServerLevel netherLevel = server.getLevel(Level.NETHER);
        if (netherLevel == null) {
            return; // walang Nether sa server na ito
        }

        ensurePlatform(netherLevel);

        Duel duel = new Duel(caster, target);
        ACTIVE.put(caster.getUUID(), duel);
        ACTIVE.put(target.getUUID(), duel);

        double cx = PLATFORM_CENTER.getX() + 0.5;
        double cz = PLATFORM_CENTER.getZ() + 0.5;
        double top = PLATFORM_CENTER.getY() + 1.0;

        teleportEntity(caster, netherLevel, cx - 1.0, top, cz, caster.getYRot(), caster.getXRot());
        teleportEntity(target, netherLevel, cx + 1.0, top, cz, target.getYRot(), target.getXRot());

        // Pigilan ang knockback MISMO sa pinagmulan niya (hindi lang i-correct ang
        // posisyon pagkatapos) — kung hindi, biglang-biglang naitutulak muna sila bago
        // tayo maka-correct, kaya mukhang "may knockback pa rin" kahit paulit-ulit nating
        // ibinabalik ang posisyon.
        setKnockbackResistance(caster, 1.0);
        setKnockbackResistance(target, 1.0);

        caster.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION_TICKS, 0, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, DURATION_TICKS, 0, false, false));
        caster.addEffect(new MobEffectInstance(MobEffects.STRENGTH, DURATION_TICKS, 1, false, false));

        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DURATION_TICKS, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, DURATION_TICKS, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, DURATION_TICKS, 1, false, false));

        netherLevel.playSound(null, cx, top, cz, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0F, 1.6F);
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        Duel duel = ACTIVE.get(entity.getUUID());
        if (duel == null) {
            return true; // walang kinalaman sa Moon God Dream, normal lang
        }

        // Payagan lang kung ang pumasok na damage ay galing sa KAPAREHA niya sa duel (caster
        // <-> target). Lahat ng iba — Nether hazards, ibang mobs, atbp. — binlock.
        LivingEntity attacker = (source.getEntity() instanceof LivingEntity le) ? le : null;
        return attacker != null
                && (attacker.getUUID().equals(duel.casterUUID) || attacker.getUUID().equals(duel.targetUUID));
    }

    private static void setKnockbackResistance(LivingEntity entity, double value) {
        AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
    }

    private static void ensurePlatform(ServerLevel netherLevel) {
        BlockPos center = PLATFORM_CENTER;
        if (netherLevel.getBlockState(center).is(Blocks.CRYING_OBSIDIAN)) {
            return; // nagawa na dati, wag nang ulitin
        }

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos floor = center.offset(dx, 0, dz);
                netherLevel.setBlockAndUpdate(floor, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                for (int dy = 1; dy <= 4; dy++) {
                    netherLevel.setBlockAndUpdate(floor.above(dy), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    // PAALALA: ito ang pinaka-malamang na kailangan ayusin gamit ang ground-truth (Ctrl+click
    // sa "teleportTo", o Ctrl+O sa loob ng klaseng ito) kapag hindi nag-compile — madalas
    // nagbabago ang cross-dimension teleport API sa iba't ibang bersyon ng Minecraft, kagaya
    // ng nangyari kay moveTo noon.
    private static void teleportEntity(LivingEntity entity, ServerLevel targetLevel,
                                       double x, double y, double z, float yRot, float xRot) {
        entity.teleportTo(targetLevel, x, y, z, Set.of(), yRot, xRot, false);
    }

    private static final class Duel {
        private final UUID casterUUID;
        private final UUID targetUUID;

        private final ServerLevel casterReturnLevel;
        private final double casterX, casterY, casterZ;
        private final float casterYRot, casterXRot;

        private final ServerLevel targetReturnLevel;
        private final double targetX, targetY, targetZ;
        private final float targetYRot, targetXRot;

        private final double casterOriginalKnockbackResistance;
        private final double targetOriginalKnockbackResistance;

        private int ticksLeft = DURATION_TICKS;

        private Duel(ServerPlayer caster, LivingEntity target) {
            this.casterUUID = caster.getUUID();
            this.targetUUID = target.getUUID();

            this.casterReturnLevel = (ServerLevel) caster.level();
            this.casterX = caster.getX();
            this.casterY = caster.getY();
            this.casterZ = caster.getZ();
            this.casterYRot = caster.getYRot();
            this.casterXRot = caster.getXRot();

            this.targetReturnLevel = (ServerLevel) target.level();
            this.targetX = target.getX();
            this.targetY = target.getY();
            this.targetZ = target.getZ();
            this.targetYRot = target.getYRot();
            this.targetXRot = target.getXRot();

            this.casterOriginalKnockbackResistance = originalKnockbackResistance(caster);
            this.targetOriginalKnockbackResistance = originalKnockbackResistance(target);
        }

        private static double originalKnockbackResistance(LivingEntity entity) {
            AttributeInstance attribute = entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            return attribute != null ? attribute.getBaseValue() : 0.0;
        }

        private boolean tick() {
            if (ticksLeft-- > 0) {
                // I-freeze ang PAREHONG participante sa eksaktong puwesto nila sa platform
                // bawat tick — hindi sila dapat gumalaw kahit saktan (parang totoong
                // nakakulong sila sa panaginip), para rin hindi sila mahulog sa anumang
                // panganib (lava, bangin) na baka nasa paligid ng platform sa Nether.
                MinecraftServer server = casterReturnLevel.getServer();
                ServerLevel netherLevel = server.getLevel(Level.NETHER);

                if (netherLevel != null) {
                    double cx = PLATFORM_CENTER.getX() + 0.5;
                    double cz = PLATFORM_CENTER.getZ() + 0.5;
                    double top = PLATFORM_CENTER.getY() + 1.0;

                    // Gamit ang mas simpleng teleportTo(x, y, z) dito (walang rotation
                    // parameters) — dahil pareho lang naman ang level (hindi cross-
                    // dimension), posisyon lang ang kailangan nating i-lock bawat tick.
                    // Kung gagamitin pa rin natin yung cross-dimension na variant (na may
                    // yRot/xRot), nasisira yung smooth camera look ng player kahit pareho
                    // lang ang pinapasa nating value, dahil "hard teleport" pa rin ang
                    // ipinapadala sa client kada tick — kaya "nakaharang"/locked din ang
                    // pagtingin niya, hindi lang ang galaw.
                    if (netherLevel.getEntity(casterUUID) instanceof LivingEntity caster) {
                        caster.teleportTo(cx - 1.0, top, cz);
                        caster.setDeltaMovement(0, 0, 0);
                    }
                    if (netherLevel.getEntity(targetUUID) instanceof LivingEntity target) {
                        target.teleportTo(cx + 1.0, top, cz);
                        target.setDeltaMovement(0, 0, 0);
                    }
                }

                return true;
            }

            MinecraftServer server = casterReturnLevel.getServer();
            ServerLevel netherLevel = server.getLevel(Level.NETHER);

            if (netherLevel != null) {
                if (netherLevel.getEntity(casterUUID) instanceof LivingEntity caster) {
                    teleportEntity(caster, casterReturnLevel, casterX, casterY, casterZ, casterYRot, casterXRot);
                    setKnockbackResistance(caster, casterOriginalKnockbackResistance);
                }
                if (netherLevel.getEntity(targetUUID) instanceof LivingEntity target) {
                    teleportEntity(target, targetReturnLevel, targetX, targetY, targetZ, targetYRot, targetXRot);
                    setKnockbackResistance(target, targetOriginalKnockbackResistance);
                }
            }

            return false;
        }
    }
}
