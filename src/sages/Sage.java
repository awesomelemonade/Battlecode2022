package sages;

import battlecode.common.*;
import sages.util.Cache;
import sages.util.Constants;
import sages.util.RunnableBot;
import sages.util.Util;

import static sages.util.Constants.rc;

public class Sage implements RunnableBot {
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.isActionReady()) {
            tryAttackMaxEcon();
        }
        if (rc.isMovementReady()) {
            Util.tryMoveAttacker();
        }
        if (rc.isActionReady()) {
            tryAttackMaxEcon();
        }
    }

    private static boolean tryAttackMaxEcon() throws GameActionException {
        RobotInfo bestRobot = null;
        double bestEcon = 0;
        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = Cache.ENEMY_ROBOTS[i];
            if (rc.canAttack(robot.location)) {
                int damage = Math.min(robot.health, RobotType.SAGE.getDamage(1));
                double econScore = getEconScore(damage, robot.type);
                if (damage == robot.health) econScore *= 1.5;
                if (econScore > bestEcon) {
                    bestEcon = econScore;
                    bestRobot = robot;
                }
            }
        }
        if (bestRobot != null) {
            rc.attack(bestRobot.location);
            return true;
        }
        return false;
    }

    public static boolean isGoingToDie(MapLocation location) {
        int potentialDamage = 0;
        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = Cache.ENEMY_ROBOTS[i];
            switch (robot.type) {
                case WATCHTOWER:
                    if (robot.mode == RobotMode.TURRET && location.isWithinDistanceSquared(robot.location, RobotType.WATCHTOWER.actionRadiusSquared)) {
                        potentialDamage += robot.type.getDamage(robot.level);
                    }
                    break;
                case SAGE:
                case SOLDIER:
                    int dx = Math.max(0, Math.abs(Cache.MY_LOCATION.x - robot.location.x) - 1);
                    int dy = Math.max(0, Math.abs(Cache.MY_LOCATION.y - robot.location.y) - 1);
                    if (dx * dx + dy * dy <= robot.type.actionRadiusSquared) {
                        // it can move then attack
                        potentialDamage += robot.type.damage;
                    }
                    break;
            }
        }
        return potentialDamage >= rc.getHealth();
    }

    public static double getChargeScore() {
        double score = 0;
        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = Cache.ENEMY_ROBOTS[i];
            if (Cache.MY_LOCATION.isWithinDistanceSquared(robot.location, Constants.ROBOT_TYPE.actionRadiusSquared)) {
                int damage = (int) (AnomalyType.CHARGE.sagePercentage * robot.type.getMaxHealth(robot.level));
                double econScore = getEconScore(damage, robot.type);
                if (damage == robot.health) econScore *= 1.5;
                score += econScore;
            }
        }
        return score;
    }

    public static double getEconScore(int damage, RobotType type) {
        return (double) damage / type.health * (type.buildCostLead + 5 * type.buildCostGold);
    }

    public static boolean tryCharge() throws GameActionException {
        if (rc.canEnvision(AnomalyType.CHARGE)) {
            rc.envision(AnomalyType.CHARGE);
            return true;
        }
        return false;
    }
}
