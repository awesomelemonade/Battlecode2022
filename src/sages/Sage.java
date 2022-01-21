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
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (rc.canAttack(robot.location)) {
                int damage = Math.min(robot.health, RobotType.SAGE.getDamage(1));
                double econScore = (double) damage / robot.type.health * (robot.type.buildCostLead + 5 * robot.type.buildCostGold);
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
}
