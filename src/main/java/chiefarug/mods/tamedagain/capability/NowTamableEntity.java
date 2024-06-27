package chiefarug.mods.tamedagain.capability;

import chiefarug.mods.tamedagain.CommonConfig;
import chiefarug.mods.tamedagain.TamingConfiguration;
import chiefarug.mods.tamedagain.goals.ITamedAgainGoalMarker;
import chiefarug.mods.tamedagain.goals.TamedEntityFollowOwnerGoal;
import chiefarug.mods.tamedagain.goals.TamedEntityOwnerHurtByTargetGoal;
import chiefarug.mods.tamedagain.goals.TamedEntityOwnerHurtTargetGoal;
import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static chiefarug.mods.tamedagain.TamedAgain.LGGR;
import static chiefarug.mods.tamedagain.capability.NowTamableEntity.AIType.BRAIN;
import static chiefarug.mods.tamedagain.capability.NowTamableEntity.AIType.GOAL;
import static chiefarug.mods.tamedagain.capability.NowTamableEntity.AIType.UNKNOWN;

public class NowTamableEntity implements ITamedEntity {
    private final LazyOptional<ITamedEntity> thisButLazy = LazyOptional.of(this);
    private UUID owner;
    private Mode mode;
    private final Mob entity;
    @Nullable
    private Collection<Goal> oldGoals;
    @Nullable
    private Collection<Goal> oldTargetSelectors;

    enum AIType {UNKNOWN, GOAL, BRAIN}
    private static final Map<EntityType<?>, AIType> entityAiCache = new HashMap<>();

    public NowTamableEntity(UUID tamer, Mode mode, Mob tamed) {
        this.owner = tamer;
        this.mode = mode;
        this.entity = tamed;
    }

    @Override
    public void forceTame(UUID owner) {
        this.owner = owner;
        this.entity.setPersistenceRequired();
    }

    @Override
    public void tame(Player owner) {
        this.owner = owner.getUUID();
        entity.level.addParticle(ParticleTypes.HEART, entity.getX(), entity.getY(), entity.getZ(), 0, 0.1, 0);

        if (owner instanceof ServerPlayer sp) {
            // manually trigger it because by default it arbitrarily restricts it to Animals.
            LootContext lootcontext = EntityPredicate.createContext(sp, entity);
            CriteriaTriggers.TAME_ANIMAL.trigger(sp, predicate -> predicate.matches(lootcontext));
        }
        setSitting(owner, true);
        spawnParticles(true);
        this.entity.setPersistenceRequired();
    }

    private void applySit() {
        entity.getNavigation().stop();

        switch (getAIType()) {
            case GOAL -> {
                oldGoals = stealStopClearGoals(entity.goalSelector);
                oldTargetSelectors = stealStopClearGoals(entity.targetSelector);
                entity.setTarget(null);
            }
            case BRAIN -> LGGR.error("Brain entity {} not implemented yet!", entity.getName().getContents());
            case UNKNOWN ->
                    LGGR.error("Weird entity that doesn't use brains or goals ({}) not implemented yet!", entity.getName().getContents());
        }
    }

    private void applyFollow() {
        AIType ai;
        ai = getAIType();

        switch (ai) {
            case GOAL -> {
                // if we somehow don't have the goals
                if (oldTargetSelectors == null || oldGoals == null) {
                    // if the goals were wiped give them back
                    if (entity.goalSelector.availableGoals.isEmpty() || entity.targetSelector.availableGoals.isEmpty()) {
                        entity.goalSelector.removeAllGoals();
                        entity.targetSelector.removeAllGoals();
                        entity.registerGoals();
                    }
                    oldGoals = stealStopClearGoals(entity.goalSelector);
                    oldTargetSelectors = stealStopClearGoals(entity.targetSelector);
                }

                TamingConfiguration configuration = TamingConfiguration.getConfiguration(entity.getType(), entity.level.registryAccess());

                insertGoalsAt(entity.targetSelector, oldTargetSelectors, configuration.targetGoalIndex(), List.of(new TamedEntityOwnerHurtByTargetGoal(getTamedEntity()), new TamedEntityOwnerHurtTargetGoal(getTamedEntity())));
                TamedEntityFollowOwnerGoal followOwnerGoal = new TamedEntityFollowOwnerGoal(getTamedEntity(), configuration);
                if (configuration.followGoalIndex().isPresent())
                    insertGoalsAt(entity.goalSelector, oldGoals, configuration.followGoalIndex().get(), List.of(followOwnerGoal));
                else
                    insertGoalsBefore(entity.goalSelector, oldGoals, RandomStrollGoal.class, 1, List.of(followOwnerGoal));


            }//TODO: How to reset brain back to good state? Do we need to just remake the brain and replace it??
            case BRAIN -> LGGR.error("Brain entity {} not implemented yet!", entity .getName().getContents());
            case UNKNOWN ->
                    LGGR.error("Weird entity that doesn't use brains or goals ({}) not implemented yet!", entity.getName().getContents());
        }
    }

    private static void insertGoalsAt(GoalSelector goalSelector, Collection<Goal> originalGoals, int index, Collection<Goal> newGoals) {
        goalSelector.removeAllGoals();
        int i = 0;
        for (Goal original : originalGoals) {
            assert !(original instanceof WrappedGoal);
            if (i == index) for (Goal newGoal : newGoals) goalSelector.addGoal(++i, newGoal);
            goalSelector.addGoal(++i, original);
        }
    }

    private static void insertGoalsBefore(GoalSelector goalSelector, Collection<Goal> originalGoals, Class<? extends Goal> target, int defaultIndex, Collection<Goal> newGoals) {
        goalSelector.removeAllGoals();
        int i = 0;
        boolean inserted = false;
        for (Goal original : originalGoals) {
            assert !(original instanceof WrappedGoal);
            if (!inserted && target.isInstance(original)) {
                for (Goal newGoal : newGoals) goalSelector.addGoal(++i, newGoal);
                inserted = true;
            }
            goalSelector.addGoal(++i, original);
        }
        // if we didn't insert any goals then abort and insert at the default position.
        // this will remove all the goals we just added but that doesn't matter
        if (!inserted)
            insertGoalsAt(goalSelector, originalGoals, defaultIndex, newGoals);
    }

    @NotNull
    private static Collection<Goal> stealStopClearGoals(GoalSelector goalSelector) {
        Collection<Goal> stolen = goalSelector.availableGoals.stream()
                .peek(WrappedGoal::stop)
                .map(WrappedGoal::getGoal)
                .filter(goal -> !(goal instanceof ITamedAgainGoalMarker))
                .toList();
        goalSelector.availableGoals.clear();
        return stolen;
    }

    private @NotNull AIType getAIType() {
        return entityAiCache.computeIfAbsent(getTamedEntity().getType(), _t -> {
            Class<?> clazz = getTamedEntity().getClass();
            do {
                boolean brain = false, goal = false;
                try {
                    clazz.getDeclaredMethod(ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_8099_")); // registerGoals
                    goal = true;
                } catch (NoSuchMethodException ignored) {}
                try {
                    clazz.getDeclaredMethod(ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, "m_6274_")); // getBrain
                    brain = true;
                } catch (NoSuchMethodException ignored) {}
                if (brain) {
                    if (goal) {
                        LGGR.error("Mob {} declares both a GOAL and BRAIN method in class {}. Unable to decide what sort of AI it uses!", getTamedEntity().getName().getString(), clazz);
                        return UNKNOWN;
                    }
                    return BRAIN;
                } else if (goal) return GOAL;
            // walk up the class hierarchy until we find a class that declares at least one of the methods, or we reach Mob in which case this entity has no AI...
            } while ((clazz = clazz.getSuperclass()) != Mob.class);
            LGGR.error("Mob {} declares no GOAL or BRAIN methods! Does it have no AI at all??", getTamedEntity().getName().getString());
            return UNKNOWN;
        });
    }

    @Override
    public boolean isSitting() {
        return mode == Mode.SIT;
    }

    @Override
    public void setSitting(@Nullable Player player, boolean sit) {
        Mode original = mode;
        mode = sit ? Mode.SIT : Mode.FOLLOW;
        if (original == mode) return; // don't do anything if the mode didn't change!

        if (sit) applySit();
        else applyFollow();

        if (player == null) return;
        MutableComponent message;
        if (CommonConfig.useTranslations.get())
            message = sit ?
                    Component.translatable("tamedagain.now_sitting.message", getTamedEntity().getDisplayName()) :
                    Component.translatable("tamedagain.now_following.message", getTamedEntity().getDisplayName());
        else message = sit ? // append everything to an empty component so that style doesn't do strange things.
                Component.empty().append(getTamedEntity().getDisplayName()).append(Component.literal(" is now sitting!")) :
                Component.empty().append(getTamedEntity().getDisplayName()).append(Component.literal(" is now following!"));
        player.displayClientMessage(message, true);
    }

    @Override // when the entity loads into game make sure to apply the correct goals.
    public void onLoad() {
        if (isTame())
            if (isSitting()) applySit();
            else applyFollow();
    }

    @Override
    public Mob getTamedEntity() {
        return entity;
    }

    @Override
    public @Nullable UUID getOwnerUUID() {
        return owner;
    }

    public enum Mode {
        SIT,
        FOLLOW;

        public ByteTag serializeNBT() {
            return ByteTag.valueOf((byte) ordinal());
        }

        public static Mode deserializeNBT(Tag nbt) {
            if (nbt instanceof NumericTag nTag)
                return values()[nTag.getAsByte()];
            return null;
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        if (owner != null)
            nbt.putUUID("owner", owner);
        if (mode != null)
            nbt.put("mode", mode.serializeNBT());
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("owner"))
            owner = nbt.getUUID("owner");
        mode = Mode.deserializeNBT(nbt.get("mode"));
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == CAPABILITY) return thisButLazy.cast();
        return LazyOptional.empty();
    }
}
