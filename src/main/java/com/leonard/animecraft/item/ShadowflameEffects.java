package com.leonard.animecraft.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ShadowflameEffects {
    private static final DustParticleOptions DARK_FLAME = new DustParticleOptions(0x2B0033, 1.6F);
    private static final float BURN_DAMAGE_PER_SECOND = 3.0F; // mas mataas kaysa normal na fire tick damage
    private static final int GROUND_FIRE_RADIUS = 2;
    private static final int MAX_SPREAD_FIRES = 24; // takip sa kalat ng BAWAT target, para hindi sumabog ang laki
    private static final int GLOBAL_MAX_FIRES = 48; // takip sa KABUUAN, kahit maraming kalaban ang na-brand
    private static final int LINGER_TICKS = 100; // 5 segundo bago talaga mawala ang ground fire
    private static final Map<UUID, Burn> ACTIVE = new ConcurrentHashMap<>();
    private static int globalFireCount = 0;

    private ShadowflameEffects() {
    }

    public static void apply(LivingEntity target, Player source, int durationTicks) {
        Burn burn = new Burn(target, source, durationTicks);

        // Kung may dati nang aktibong burn sa PAREHONG target, patayin muna yung mga naunang
        // Soul Fire/Soul Sand niya bago tayo gumawa ng bago — kung hindi, maiiwan silang
        // nag-aapoy habang-buhay dahil mawawala na sa tracking ang listahan nila.
        Burn previous = ACTIVE.put(target.getUUID(), burn);
        if (previous != null) {
            previous.extinguishGround();
        }

        if (target.level() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(GameRules.MOB_GRIEFING)) {
            burn.igniteGround(serverLevel);
        }
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<Map.Entry<UUID, Burn>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                if (!it.next().getValue().tick()) {
                    it.remove();
                }
            }
        });
    }

    private static boolean isFlammable(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.WOOL) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_STAIRS);
    }

    /**
     * Isang nasindihang puwesto: yung fire block mismo (sa itaas), at yung puwesto sa ibaba nito
     * na ginawang Soul Sand para "mabuhay" ang Soul Fire. Para sa ground ignite, yung "groundPos"
     * ay yung lupa/damo sa ilalim; para sa spread sa kahoy, yung "groundPos" ay yung kahoy mismo
     * na na-corrupt/naging Soul Sand. Itinatago rito kung ano talaga ang orihinal na laman ng
     * groundPos bago natin binago, para maibalik natin pagtapos ng epekto.
     */
    private record FireSite(BlockPos firePos, BlockPos groundPos, BlockState originalGround) {
    }

    private static final class Burn {
        private final LivingEntity target;
        private final Player source;
        private final List<FireSite> groundFire = new ArrayList<>();
        private int ticksLeft;
        private int lingerLeft = LINGER_TICKS;

        private Burn(LivingEntity target, Player source, int ticksLeft) {
            this.target = target;
            this.source = source;
            this.ticksLeft = ticksLeft;
        }

        private void igniteGround(ServerLevel serverLevel) {
            BlockPos center = target.blockPosition();

            for (int dx = -GROUND_FIRE_RADIUS; dx <= GROUND_FIRE_RADIUS; dx++) {
                for (int dz = -GROUND_FIRE_RADIUS; dz <= GROUND_FIRE_RADIUS; dz++) {
                    if (dx * dx + dz * dz > GROUND_FIRE_RADIUS * GROUND_FIRE_RADIUS) {
                        continue;
                    }

                    tryIgniteEmpty(serverLevel, center.offset(dx, 0, dz));
                }
            }
        }

        /** True kapag ang puwestong ito ay fire o ground site na (galing man sa lupa o kahoy). */
        private boolean isTracked(BlockPos pos) {
            for (FireSite site : groundFire) {
                if (site.firePos().equals(pos) || site.groundPos().equals(pos)) {
                    return true;
                }
            }
            return false;
        }

        private boolean tryIgniteEmpty(ServerLevel serverLevel, BlockPos pos) {
            if (groundFire.size() >= MAX_SPREAD_FIRES || globalFireCount >= GLOBAL_MAX_FIRES
                    || !serverLevel.isEmptyBlock(pos) || isTracked(pos)) {
                return false;
            }

            BlockPos below = pos.below();
            if (isTracked(below) || !serverLevel.getBlockState(below).isFaceSturdy(serverLevel, below, Direction.UP)) {
                return false;
            }

            return placeSoulFire(serverLevel, pos, below);
        }

        private boolean placeSoulFire(ServerLevel serverLevel, BlockPos firePos, BlockPos groundPos) {
            if (groundFire.size() >= MAX_SPREAD_FIRES || globalFireCount >= GLOBAL_MAX_FIRES) {
                return false;
            }

            BlockState originalGround = serverLevel.getBlockState(groundPos);
            if (!originalGround.is(Blocks.SOUL_SAND) && !originalGround.is(Blocks.SOUL_SOIL)) {
                serverLevel.setBlockAndUpdate(groundPos, Blocks.SOUL_SAND.defaultBlockState());
            }

            serverLevel.setBlockAndUpdate(firePos, Blocks.SOUL_FIRE.defaultBlockState());
            groundFire.add(new FireSite(firePos.immutable(), groundPos.immutable(), originalGround));
            globalFireCount++;
            return true;
        }

        private void trySpread(ServerLevel serverLevel) {
            if (groundFire.size() >= MAX_SPREAD_FIRES || globalFireCount >= GLOBAL_MAX_FIRES) {
                return;
            }

            // Kopya ng listahan dahil babaguhin natin yung groundFire habang umiikot dito.
            List<FireSite> current = new ArrayList<>(groundFire);
            for (FireSite site : current) {
                for (Direction direction : Direction.values()) {
                    BlockPos woodPos = site.firePos().relative(direction);
                    if (isTracked(woodPos) || !isFlammable(serverLevel.getBlockState(woodPos))) {
                        continue;
                    }

                    // Yung kahoy mismo ang magiging Soul Sand (parang na-abo/na-corrupt), at
                    // susulpot ang purple flame sa ibabaw nito — kaya dapat bakante ang puwesto
                    // sa taas nito.
                    BlockPos above = woodPos.above();
                    if (isTracked(above) || !serverLevel.isEmptyBlock(above)) {
                        continue;
                    }

                    placeSoulFire(serverLevel, above, woodPos);
                }
            }
        }

        private boolean tick() {
            if (target.isAlive() && ticksLeft-- > 0) {
                // Panatilihing may normal fire visual bilang base (hindi natin mababago yung kulay
                // ng "nasusunog" overlay sa katawan nang walang mixin), pero yung sariling dark
                // particles + damage + purple na ground fire (Soul Fire, re-textured) natin ang
                // nagpapakita ng "cursed" na bersyon.
                target.igniteForSeconds(2);

                if (target.level() instanceof ServerLevel serverLevel) {
                    if (ticksLeft % 20 == 0) {
                        DamageSource burnSource = serverLevel.damageSources().playerAttack(source);
                        target.hurtServer(serverLevel, burnSource, BURN_DAMAGE_PER_SECOND);
                    }

                    double x = target.getX();
                    double y = target.getY() + target.getBbHeight() * 0.5;
                    double z = target.getZ();
                    serverLevel.sendParticles(DARK_FLAME, x, y, z, 4, 0.3, 0.4, 0.3, 0.02);

                    if (ticksLeft % 10 == 0) {
                        trySpread(serverLevel);
                    }
                }

                return true;
            }

            // Patay na o naubusan na ng oras ang target, pero hayaan munang magningas ng ilang
            // segundo yung ground fire bago talaga papawiin — para hindi biglaan mawala.
            if (lingerLeft-- > 0) {
                return true;
            }

            extinguishGround();
            return false;
        }

        private void extinguishGround() {
            if (groundFire.isEmpty()) {
                return; // naka-extinguish na dati (hal. na-re-brand yung target), iwas double-count
            }

            if (target.level() instanceof ServerLevel serverLevel) {
                for (FireSite site : groundFire) {
                    if (serverLevel.getBlockState(site.firePos()).is(Blocks.SOUL_FIRE)) {
                        serverLevel.removeBlock(site.firePos(), false);
                    }
                    if (!site.originalGround().is(Blocks.SOUL_SAND)
                            && serverLevel.getBlockState(site.groundPos()).is(Blocks.SOUL_SAND)) {
                        serverLevel.setBlockAndUpdate(site.groundPos(), site.originalGround());
                    }
                }
            }

            globalFireCount = Math.max(0, globalFireCount - groundFire.size());
            groundFire.clear();
        }
    }
}
