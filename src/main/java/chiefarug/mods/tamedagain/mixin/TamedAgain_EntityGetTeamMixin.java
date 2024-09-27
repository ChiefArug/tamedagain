package chiefarug.mods.tamedagain.mixin;

import chiefarug.mods.tamedagain.capability.ITamedEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
@SuppressWarnings("UnstableApiUsage")
public abstract class TamedAgain_EntityGetTeamMixin extends CapabilityProvider<Entity> {

    @Shadow public Level level;

    private TamedAgain_EntityGetTeamMixin() {
        super(Entity.class);
    }

    // TODO: Can this be changed to use getOwnerUUID instead of getOwner so it works when the owner is offline?
    // TODO: Sync the capability (optionally, so that vanilla can still connect) so that this works client side for
    //  glowing effect rendering and minimap stuff (im guessing minimaps show team stuff).
    @Inject(method = "getTeam",at = @At("HEAD"), cancellable = true, require = 1)
    public void tamedagain$getTeam(CallbackInfoReturnable<Team> cir) {
        if (level.isClientSide) return; // currently it is not synced so it is pointless checking on client side.
        getCapability(ITamedEntity.CAPABILITY)
                .filter(ITamedEntity::isTame)
                .map(ITamedEntity::getOwnerUUID)
                // because we call this by uuid it will NOT stall the server and wait for a
                // response if the uuid is not in the cache. this is very important, even though
                // there should never be a situation where the owner is not in the cache.
                .flatMap(level.getServer().getProfileCache()::get)
                .map(GameProfile::getName)
                .map(level.getScoreboard()::getPlayersTeam)
                .ifPresent(cir::setReturnValue);
    }
}
