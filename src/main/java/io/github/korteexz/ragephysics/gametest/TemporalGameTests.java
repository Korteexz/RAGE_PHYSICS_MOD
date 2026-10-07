package io.github.korteexz.ragephysics.gametest;

import io.github.korteexz.ragephysics.RagePhysics;
import io.github.korteexz.ragephysics.selection.PlayerSelection;
import io.github.korteexz.ragephysics.selection.SelectionManager;
import io.github.korteexz.ragephysics.temporal.TemporalConfig;
import io.github.korteexz.ragephysics.temporal.TemporalEntityTicker;
import io.github.korteexz.ragephysics.temporal.TemporalRegionResolver;
import io.github.korteexz.ragephysics.temporal.TemporalTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** NeoForge só registra estes testes em ambiente de desenvolvimento. Não toca mundos normais automaticamente. */
@GameTestHolder(RagePhysics.MODID)
@PrefixGameTestTemplate(false)
public final class TemporalGameTests {
    @GameTest(template = "temporal_empty", timeoutTicks = 100)
    public static void entitiesAndIsolation(GameTestHelper helper) {
        var level = helper.getLevel();
        Player owner = helper.makeMockPlayer(GameType.CREATIVE);
        Player otherOwner = helper.makeMockPlayer(GameType.CREATIVE);
        PlayerSelection selection = SelectionManager.get(owner);
        BlockPos origin = helper.absolutePos(new BlockPos(3, 1000, 3));
        BlockPos a = origin.offset(-100, -10000, -100);
        BlockPos b = origin.offset(100, 10000, 100);
        try {
            selection.select(level.dimension(), a);
            selection.select(level.dimension(), b);
            check(selection.updateTimeScale(selection.getRevision(), .25), "selection update");
            check(TemporalRegionResolver.resolve(level.dimension(), origin, TemporalTarget.PROJECTILES) == selection, "inside region");
            check(TemporalRegionResolver.resolve(Level.NETHER, origin, TemporalTarget.PROJECTILES) == null, "dimension isolation");
            check(TemporalRegionResolver.resolve(level.dimension(), b.east(), TemporalTarget.PROJECTILES) == null, "outside region");
            check(!TemporalConfig.enabled(TemporalTarget.PLAYERS), "players remain vanilla");
            check(TemporalTarget.classify(owner) == TemporalTarget.PLAYERS, "exclusive player category");
            check(SelectionManager.get(otherOwner).getTimeScale() == 1, "independent authorship");
            for (double invalid : new double[] {0, -1, .124, 8.001, Double.NaN, Double.POSITIVE_INFINITY}) {
                check(!selection.updateTimeScale(selection.getRevision(), invalid), "reject invalid scale");
            }

            // O dispatcher transformado pelo Mixin deve produzir a mesma trajetória que N ticks vanilla.
            for (double[] sample : new double[][] {{.25, 80, 20}, {4, 20, 80}, {2.5, 40, 100}, {.17, 100, 17}, {1, 40, 40}}) {
                selection.updateTimeScale(selection.getRevision(), sample[0]);
                Arrow actual = arrow(level, origin);
                Arrow reference = arrow(level, origin);
                check(TemporalTarget.classify(actual) == TemporalTarget.PROJECTILES, "exclusive projectile category");
                for (int i = 0; i < sample[1]; i++) level.tickNonPassenger(actual);
                for (int i = 0; i < sample[2]; i++) {
                    reference.setOldPosAndRot();
                    reference.tickCount++;
                    reference.tick();
                }
                check(actual.tickCount == (int) sample[2], "local projectile steps " + sample[0]);
                check(actual.position().distanceToSqr(reference.position()) < 1e-12, "trajectory " + sample[0]);
                check(actual.getDeltaMovement().distanceToSqr(reference.getDeltaMovement()) < 1e-12, "gravity/drag " + sample[0]);
                TemporalEntityTicker.remove(actual);
            }

            selection.updateTimeScale(selection.getRevision(), .25);
            Zombie zombie = EntityType.ZOMBIE.create(level);
            zombie.setPos(Vec3.atCenterOf(origin));
            zombie.setNoGravity(true);
            zombie.setNoAi(true);
            zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100));
            check(TemporalTarget.classify(zombie) == TemporalTarget.LIVING_ENTITIES, "exclusive living category");
            for (int i = 0; i < 40; i++) level.tickNonPassenger(zombie);
            check(zombie.tickCount == 10, "mob local steps");
            check(zombie.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getDuration() == 90, "mob timer progression");
            zombie.setPos(Vec3.atCenterOf(b.east()));
            level.tickNonPassenger(zombie);
            check(zombie.tickCount == 11, "exit immediately returns to vanilla");
            TemporalEntityTicker.remove(zombie);

            ModConfigSpec.BooleanValue projectiles = TemporalConfig.SPEC.getValues().get("categories.projectiles");
            boolean previous = projectiles.get();
            try {
                projectiles.set(false);
                Arrow disabled = arrow(level, origin);
                for (int i = 0; i < 4; i++) level.tickNonPassenger(disabled);
                check(disabled.tickCount == 4, "disabled category bypasses scaling");
            } finally {
                projectiles.set(previous);
            }

            PlayerSelection second = SelectionManager.get(otherOwner);
            second.select(level.dimension(), a);
            second.select(level.dimension(), b);
            second.updateTimeScale(second.getRevision(), 4);
            PlayerSelection winner = owner.getUUID().compareTo(otherOwner.getUUID()) < 0 ? selection : second;
            check(TemporalRegionResolver.resolve(level.dimension(), origin, TemporalTarget.PROJECTILES) == winner, "deterministic overlap");
            long oldRevision = selection.getRevision();
            selection.select(Level.NETHER, origin);
            check(!selection.isComplete() && selection.getTimeScale() == 1, "cross-dimension click resets selection");
            selection.select(Level.NETHER, origin.above());
            check(!selection.updateTimeScale(oldRevision, 8), "old screen cannot edit new region");
        } finally {
            SelectionManager.remove(owner);
            SelectionManager.remove(otherOwner);
        }
        helper.succeed();
    }

    @GameTest(template = "temporal_empty", timeoutTicks = 100)
    public static void furnaceDispatchers(GameTestHelper helper) {
        Player slowOwner = helper.makeMockPlayer(GameType.CREATIVE);
        Player fastOwner = helper.makeMockPlayer(GameType.CREATIVE);
        AbstractFurnaceBlockEntity slow = furnace(helper, new BlockPos(1, 2, 2));
        AbstractFurnaceBlockEntity normal = furnace(helper, new BlockPos(3, 2, 2));
        AbstractFurnaceBlockEntity fast = furnace(helper, new BlockPos(5, 2, 2));
        setRegion(slowOwner, slow.getBlockPos(), .25);
        setRegion(fastOwner, fast.getBlockPos(), 4);
        helper.runAfterDelay(40, () -> {
            try {
                int slowCook = slow.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("CookTime");
                int normalCook = normal.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("CookTime");
                int fastCook = fast.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("CookTime");
                check(normalCook >= 38 && normalCook <= 42, "vanilla furnace: " + normalCook);
                check(slowCook >= 8 && slowCook <= 12, "slow furnace: " + slowCook);
                check(fastCook >= 152 && fastCook <= 168, "fast furnace: " + fastCook);
                helper.succeed();
            } finally {
                SelectionManager.remove(slowOwner);
                SelectionManager.remove(fastOwner);
            }
        });
    }

    private static Arrow arrow(net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        Arrow arrow = new Arrow(EntityType.ARROW, level);
        arrow.setPos(Vec3.atCenterOf(pos));
        arrow.setDeltaMovement(.1, .15, 0);
        return arrow;
    }

    private static AbstractFurnaceBlockEntity furnace(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.FURNACE);
        var furnace = (AbstractFurnaceBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        furnace.setItem(0, new ItemStack(Items.RAW_IRON, 64));
        furnace.setItem(1, new ItemStack(Items.COAL, 4));
        return furnace;
    }

    private static void setRegion(Player owner, BlockPos pos, double scale) {
        PlayerSelection selection = SelectionManager.get(owner);
        selection.select(owner.level().dimension(), pos);
        selection.select(owner.level().dimension(), pos);
        selection.updateTimeScale(selection.getRevision(), scale);
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private TemporalGameTests() {}
}
