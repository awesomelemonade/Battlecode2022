package faceit7;

import battlecode.common.*;
import faceit7.util.*;

import static faceit7.util.Constants.rc;

public class Soldier implements RunnableBot {
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        Communication.setSoldierHealthInfo(rc.getHealth());
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
        if (rc.isMovementReady()) {
            Util.tryMoveAttacker();
        }
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
    }

    public static void tryAttackLowHealth() throws GameActionException {
        RobotInfo bestRobot = null;
        double bestScore = Double.MAX_VALUE;
        // Prioritize by unit type, then health(, then rubble?)
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (rc.canAttack(robot.location)) {
                double score = getUnitTypePriority(robot.type) * 10000 + robot.health;
                if (score < bestScore) {
                    bestScore = score;
                    bestRobot = robot;
                }
            }
        }
        if (bestRobot != null) {
            rc.attack(bestRobot.location);
        }
    }

    // Prioritize soldiers = faceit7 = watchtowers > miners > builders > archons > laboratories
    public static int getUnitTypePriority(RobotType type) {
        switch (type) {
            case SOLDIER:
            case SAGE:
            case WATCHTOWER:
                return 1;
            case MINER:
                return 2;
            case BUILDER:
                return 3;
            case ARCHON:
                return 4;
            case LABORATORY:
                return 5;
            default:
                throw new IllegalArgumentException("Unknown Unit Type");
        }
    }
}
