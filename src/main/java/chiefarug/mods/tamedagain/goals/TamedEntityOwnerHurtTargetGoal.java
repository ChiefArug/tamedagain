package chiefarug.mods.tamedagain.goals;

import chiefarug.mods.tamedagain.capability.ITamedEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class TamedEntityOwnerHurtTargetGoal extends TargetGoal implements ITamedAgainGoalMarker {
    private final Mob entity;
    private LivingEntity ownerLastHurt;
    private int timestamp;

    public TamedEntityOwnerHurtTargetGoal(Mob entity) {
        super(entity, false);
        this.entity = entity;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    public boolean canUse() {
        ITamedEntity cap = this.entity.getCapability(ITamedEntity.CAPABILITY).resolve().get();
        if (cap.isTame() && !cap.isSitting()) {
            Player owner = cap.get().getOwner();
            if (owner == null) return false;
            else {
                this.ownerLastHurt = owner.getLastHurtMob();
                if (owner.getLastHurtMobTimestamp() != this.timestamp &&
                        this.canAttack(this.ownerLastHurt, TargetingConditions.DEFAULT)
                ) return true;
                else if (this.ownerLastHurt != null) return this.ownerLastHurt.getCapability(ITamedEntity.CAPABILITY)
                        .resolve()
                        .filter(iTamedEntity -> iTamedEntity.getOwner() != owner)
                        .isPresent();
            }
        }
        return false;
    }

    public void start() {
        this.mob.setTarget(this.ownerLastHurt);
        Player owner = this.entity.getCapability(ITamedEntity.CAPABILITY).resolve().get().getOwner();
        if (owner != null) this.timestamp = owner.getLastHurtMobTimestamp();
        super.start();
    }
}
