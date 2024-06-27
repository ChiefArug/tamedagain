package chiefarug.mods.tamedagain.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.NonNullSupplier;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static chiefarug.mods.tamedagain.TamedAgain.MODRL;
import static net.minecraft.core.particles.ParticleTypes.HEART;
import static net.minecraft.core.particles.ParticleTypes.SMOKE;

@AutoRegisterCapability
public interface ITamedEntity extends ICapabilitySerializable<CompoundTag>, NonNullSupplier<ITamedEntity> {
    ResourceLocation ID = MODRL.withPath("tame_data");
    Capability<ITamedEntity> CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    static void attachToEntity(AttachCapabilitiesEvent<Entity> event) {
        // You cannot tame players. No I am not implementing that.
        if (event.getObject() instanceof Mob mob && !mob.level.isClientSide) {
            if (mob instanceof TamableAnimal ta) {
                event.addCapability(ID, new AlreadyTamableEntity(ta));
                return;
            }
            event.addCapability(ID, new NowTamableEntity(null, null, mob));
        }
    }

    @Override
    default CompoundTag serializeNBT() {
        return new CompoundTag();
    }

    @Override
    default void deserializeNBT(CompoundTag compoundTag) {}

    @Override
    default @NotNull ITamedEntity get() {
        return this;
    }

    void forceTame(UUID owner);

    void tame(Player owner);

    boolean isSitting();

    void setSitting(@Nullable Player player, boolean sit);

    default void spawnParticles(boolean tameSuccessful) {
        Mob entity = getTamedEntity();
        if (entity.level instanceof ServerLevel level) {
            level.sendParticles(tameSuccessful ? HEART : SMOKE, entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ(), 8, 0.5, 0.5, 0.5, 0.1);
        }
    }

    default boolean isTame() {
        return getOwnerUUID() != null;
    }

    @Nullable
    default Player getOwner() {
        UUID uuid = getOwnerUUID();

        return uuid == null ? null : getTamedEntity().level.getPlayerByUUID(uuid);
    }

    Mob getTamedEntity();

    @Nullable
    UUID getOwnerUUID();

    default void toggleSitting(Player player) {
        setSitting(player, !isSitting());
    }

    default void onLoad() {}
}
