package chiefarug.mods.tamedagain.capability;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record AlreadyTamableEntity(TamableAnimal tamableAnimal) implements ITamedEntity {
    @Override
    public void forceTame(UUID owner) {
        tamableAnimal.setTame(true);
        tamableAnimal.setOwnerUUID(owner);
    }

    @Override
    public void tame(Player owner) {
        tamableAnimal.tame(owner);
        spawnParticles(true);
    }

    @Override
    public boolean isSitting() {
        return tamableAnimal.isOrderedToSit();
    }

    @Override
    public void setSitting(@Nullable Player player, boolean sit) {
        tamableAnimal.setOrderedToSit(sit);
    }

    @Override
    public @Nullable Player getOwner() {
        LivingEntity owner = tamableAnimal.getOwner();
        if (owner instanceof Player player) return player;
        return null;
    }

    @Override
    public Mob getTamedEntity() {
        return tamableAnimal;
    }

    @Override
    public @Nullable UUID getOwnerUUID() {
        return tamableAnimal.getOwnerUUID();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        if (capability == CAPABILITY)
            return LazyOptional.of(this).cast();
        return LazyOptional.empty();
    }
}
