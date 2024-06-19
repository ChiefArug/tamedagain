package chiefarug.mods.tamedagain.goals;

import chiefarug.mods.tamedagain.capability.ITamedEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.Optional;

public class TamedEntityOwnerHurtByTargetGoal extends TargetGoal implements ITamedAgainGoalMarker {
    private final Mob self;
    private LivingEntity ownerLastHurtBy;
    private int timestamp;

    public  TamedEntityOwnerHurtByTargetGoal(Mob pTameAnimal) {
        super(pTameAnimal, false);
        this.self = pTameAnimal;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    public boolean canUse() {
        Optional<ITamedEntity> cap = this.self.getCapability(ITamedEntity.CAPABILITY).resolve();
        if (cap.isEmpty()) return false;

        if (cap.get().isTame() && !cap.get().isSitting()) {
            Player owner = cap.get().getOwner();
            if (owner == null) return false;
            else {
                this.ownerLastHurtBy = owner.getLastHurtByMob();
                if (owner.getLastHurtByMobTimestamp() != this.timestamp &&
                        this.canAttack(this.ownerLastHurtBy, TargetingConditions.DEFAULT)
                ) return true;
                else if (this.ownerLastHurtBy != null)
                    return this.ownerLastHurtBy.getCapability(ITamedEntity.CAPABILITY)
                            .resolve()
                            .filter(iTamedEntity -> iTamedEntity.getOwner() != owner)
                            .isPresent();
            }
        }
        return false;
    }

    public void start() {
        this.mob.setTarget(this.ownerLastHurtBy);
        Player owner = this.self.getCapability(ITamedEntity.CAPABILITY).resolve().get().getOwner();
        if (owner != null) this.timestamp = owner.getLastHurtByMobTimestamp();
        super.start();
    }
}
