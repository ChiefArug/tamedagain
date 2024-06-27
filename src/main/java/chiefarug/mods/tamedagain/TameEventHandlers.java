package chiefarug.mods.tamedagain;

import chiefarug.mods.tamedagain.capability.AlreadyTamableEntity;
import chiefarug.mods.tamedagain.capability.ITamedEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static chiefarug.mods.tamedagain.TamedAgain.LGGR;
import static chiefarug.mods.tamedagain.TamedAgain.MODID;
import static chiefarug.mods.tamedagain.capability.ITamedEntity.CAPABILITY;

@Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TameEventHandlers {
    @SubscribeEvent
    public static void onEntityClick(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Mob entity) || event.getTarget().level.isClientSide) return;

        Optional<ITamedEntity> rawCap = entity.getCapability(CAPABILITY).resolve();
        if (rawCap.isEmpty()) return;
        ITamedEntity cap = rawCap.get();
        Player player = event.getEntity();
        ItemStack item = player.getItemInHand(event.getHand());

        toCancel:
        if (cap.isTame()) {
            // if its already tamable then they handle this. Because we are doing this server side we also need to only bother checking the main hand.
            if (cap instanceof AlreadyTamableEntity || event.getHand() != InteractionHand.MAIN_HAND) return;

            cap.toggleSitting(player);
            player.swing(event.getHand(), true);
        } else {

            EntityType<?> type = entity.getType();

            toTame:
            if (CommonConfig.enableStaff.get() && item.is(TamedAgain.STAFF)) {
                if (type.is(TamedAgain.STAFF_TAME_BLACKLIST)) return;
                if (player.isCreative()) break toTame;
                float health = CommonConfig.staffXpCostUseCurrentHealth.get() ? entity.getHealth() : entity.getMaxHealth();
                int requiredXpLevels = (int) Math.ceil(CommonConfig.staffXpCost.get() * health);
                if (player.experienceLevel >= requiredXpLevels) {
                    player.giveExperienceLevels(-requiredXpLevels);
                    break toTame;
                }
                player.displayClientMessage(CommonConfig.useTranslations.get() ?
                                Component.translatable("tamedagain.not_enough__xp_levels.message", entity.getDisplayName(), String.valueOf(requiredXpLevels)) :
                                Component.literal("Not enough xp levels to tame ").append(entity.getDisplayName()).append("! You need " + requiredXpLevels + " levels"),
                        true);
                break toCancel;
            } else {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(type);
                @Nullable
                TamingFood food = event.getLevel().getServer().registryAccess().registryOrThrow(TamingFood.REGISTRY_KEY).get(entityId);
                if (food == null || !food.validItems().test(item)) return;

                entity.playSound(entity.getEatingSound(item));
                item.shrink(1);

                if (entity.getRandom().nextDouble() > food.chance()) {
                    cap.spawnParticles(false);
                    break toCancel;
                }
            }
            LGGR.debug("{} is taming {}!", player.getScoreboardName(), entity.getName().getContents());
            cap.tame(player);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityTransform(LivingConversionEvent.Post event) {
        Optional<ITamedEntity> source = event.getEntity().getCapability(CAPABILITY).resolve();
        Optional<ITamedEntity> destination = event.getOutcome().getCapability(CAPABILITY).resolve();

        if (source.isPresent() && destination.isPresent()) {
            UUID sourceOwner = source.get().getOwnerUUID();
            destination.get().forceTame(sourceOwner);
            destination.get().setSitting(null, source.get().isSitting());
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob entity) || entity.level.isClientSide) return;
        LazyOptional<ITamedEntity> cap = entity.getCapability(CAPABILITY);
        cap.ifPresent(ITamedEntity::onLoad);
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Mob targetEntity) || event.getEntity().level.isClientSide) return;
        targetEntity.getCapability(CAPABILITY)
                .filter(ITamedEntity::isTame)
                .ifPresent(cap -> cap.setSitting(null, false));
    }

    //TODO: Change this to use teams instead of UUIDS
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onTargetSelected(LivingChangeTargetEvent event) {
        /* rant: {
                so, in an event called LivingChangeTargetEvent what do you expect getOriginalTarget and getNewTarget to be?
                you probably expect getOriginalTarget to return the original target, before the event was fired to change it
                and getNewTarget to return the new target that it is being changed to.
                well NO. That is not what they are.
                For all intents and purposes, without any other mods doing anything, getOriginalTarget == getNewTarget.
                Original refers to the Original change, before mods messed with it, not to the original target, before it changed.
                I SPENT HOURS DEBUGGING THIS.
                break rant;
        }*/
        LivingEntity attacker = event.getEntity();
        LivingEntity newTarget = event.getNewTarget();

        if (event.getEntity().level.isClientSide || newTarget == null) return;


        // don't allow tamed mobs to attack their owner, anyone owned by their owner or
        // anyone owned by anyone allied with their owner
        Optional<UUID> attackerOwner = attacker.getCapability(CAPABILITY)
                .resolve()
                .map(ITamedEntity::getOwnerUUID);
        if (attackerOwner.isEmpty()) return;
        Optional<UUID> targetOwner = newTarget.getCapability(CAPABILITY)
                .resolve()
                .map(ITamedEntity::getOwnerUUID);
        if (targetOwner.isEmpty()) return;

        if (attackerOwner.get().equals(targetOwner.get())) {
            event.setCanceled(true);
        } else {
            Scoreboard scoreboard = attacker.level.getScoreboard();
            assert attacker.level instanceof ServerLevel;
            GameProfileCache profiles = attacker.level.getServer().getProfileCache();

            Optional<PlayerTeam> attackerOwnerTeam = attackerOwner
                    .flatMap(profiles::get)
                    .map(GameProfile::getName)
                    .map(scoreboard::getPlayersTeam);
            if (attackerOwnerTeam.isEmpty()) return;

            Optional<PlayerTeam> targetOwnerTeam = targetOwner
                    .flatMap(profiles::get)
                    .map(GameProfile::getName)
                    .map(scoreboard::getPlayersTeam);
            if (targetOwnerTeam.isEmpty()) return;

            if (attackerOwnerTeam.get().isAlliedTo(targetOwnerTeam.get())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void entityTickDebug(LivingEvent.LivingTickEvent event) {
//        if (event.getEntity().level.isClientSide) return;
//        if (event.getEntity() instanceof Husk entity) {
//            LGGR.debug("set breakpoint here");
//        }
    }

    @SubscribeEvent // mobs hurt by tamed mobs will drop player drops
    public static void onLivingHurt(LivingHurtEvent event) {
        event.getEntity().getCapability(CAPABILITY)
                .filter(ITamedEntity::isTame)
                .ifPresent(cap -> {
                    LivingEntity hurtEntity = event.getEntity();
                    hurtEntity.lastHurtByPlayerTime = 100;
                    Player owner = cap.getOwner();
                    hurtEntity.lastHurtByPlayer = owner;
                });
    }


    //TODO: Death event and show death message?
}
