package chiefarug.mods.tamedagain.mixin;

import chiefarug.mods.tamedagain.capability.ITamedEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
@SuppressWarnings("UnstableApiUsage")
public abstract class TamedAgain_EntityGetTeamMixin extends CapabilityProvider<Entity> {

    protected TamedAgain_EntityGetTeamMixin() {
        super(Entity.class);
    }

    // TODO: Can this be changed to use getOwnerUUID instead of getOwner so it works when the owner is offline?
    // TODO: Sync the capability (optionally, so that vanilla can still connect) so that this works client side for
    //  glowing effect rendering and minimap stuff (im guessing minimaps show team stuff).
    @Inject(method = "getTeam",at = @At("HEAD"), cancellable = true)
    public void tamedagain$getTeam(CallbackInfoReturnable<Team> cir) {
        getCapability(ITamedEntity.CAPABILITY)
                .filter(ITamedEntity::isTame)
                .map(ITamedEntity::getOwner)
                .map(Player::getTeam)
                .ifPresent(cir::setReturnValue);
    }
}
