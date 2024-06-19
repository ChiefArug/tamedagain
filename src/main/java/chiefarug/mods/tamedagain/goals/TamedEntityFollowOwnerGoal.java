package chiefarug.mods.tamedagain.goals;

import chiefarug.mods.tamedagain.capability.ITamedEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;

public class TamedEntityFollowOwnerGoal extends Goal implements ITamedAgainGoalMarker {
    private final Mob entity;
    private final ITamedEntity cap;
    private Player owner;
    private final LevelReader level;
    private final double speedModifier;
    private final PathNavigation navigation;
    private int timeToRecalcPath;
    private final float stopDistance;
    private final float startDistance;
    private float oldWaterCost;

    public TamedEntityFollowOwnerGoal(Mob entity, double speedModifier, float startDistance, float stopDistance) {
        this.entity = entity;
        this.cap = entity.getCapability(ITamedEntity.CAPABILITY).resolve().get();
        this.level = entity.level;
        this.speedModifier = speedModifier;
        this.navigation = entity.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        Player owner = cap.getOwner();
        if (owner == null) {
            return false;
        } else if (owner.isSpectator()) {
            return false;
        } else if (cap.isSitting()) {
            return false;
        } else if (this.entity.distanceToSqr(owner) < (double) (this.startDistance * this.startDistance)) {
            return false;
        } else {
            this.owner = owner;
            return true;
        }
    }

    public boolean canContinueToUse() {
        if (this.navigation.isDone()) {
            return false;
        } else if (cap.isSitting()) {
            return false;
        } else {
            return !(this.entity.distanceToSqr(this.owner) <= (double) (this.stopDistance * this.stopDistance));
        }
    }

    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.entity.getPathfindingMalus(BlockPathTypes.WATER);
        this.entity.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.entity.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
    }

    public void tick() {
        this.entity.getLookControl().setLookAt(this.owner, 10.0F, (float) this.entity.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = this.adjustedTickDelay(10);
            if (!this.entity.isLeashed() && !this.entity.isPassenger()) {
                if (this.entity.distanceToSqr(this.owner) >= FollowOwnerGoal.TELEPORT_WHEN_DISTANCE_IS * FollowOwnerGoal.TELEPORT_WHEN_DISTANCE_IS)
                    this.teleportToOwner();
                else
                    this.navigation.moveTo(this.owner, this.speedModifier);
            }
        }
    }

    private void teleportToOwner() {
        BlockPos ownerPos = this.owner.blockPosition();

        for (int i = 0; i < 10; ++i) {
            int x = this.randomIntInclusive(-FollowOwnerGoal.MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING, FollowOwnerGoal.MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING);
            int y = this.randomIntInclusive(-FollowOwnerGoal.MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING, FollowOwnerGoal.MAX_VERTICAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING);
            int z = this.randomIntInclusive(-FollowOwnerGoal.MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING, FollowOwnerGoal.MAX_HORIZONTAL_DISTANCE_FROM_PLAYER_WHEN_TELEPORTING);
            if (this.maybeTeleportTo(ownerPos.getX() + x, ownerPos.getY() + y, ownerPos.getZ() + z))
                break;
        }
    }

    private boolean maybeTeleportTo(int pX, int pY, int pZ) {
        if (Math.abs((double) pX - this.owner.getX()) < 2.0 && Math.abs((double) pZ - this.owner.getZ()) < 2.0) {
            return false;
        } else if (!this.canTeleportTo(new BlockPos(pX, pY, pZ))) {
            return false;
        } else {
            this.entity.moveTo((double) pX + 0.5, (double) pY, (double) pZ + 0.5, this.entity.getYRot(), this.entity.getXRot());
            this.navigation.stop();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPos targetPos) {
        BlockPathTypes pathType = WalkNodeEvaluator.getBlockPathTypeStatic(this.level, targetPos.mutable());
        if (pathType != BlockPathTypes.WALKABLE) {
            return false;
        } else {
            BlockPos pos = targetPos.subtract(this.entity.blockPosition());
            return this.level.noCollision(this.entity, this.entity.getBoundingBox().move(pos));
        }
    }

    private int randomIntInclusive(int pMin, int pMax) {
        return this.entity.getRandom().nextInt(pMax - pMin + 1) + pMin;
    }
}
