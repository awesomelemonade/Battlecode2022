package aggroworkers;

import battlecode.common.*;
import aggroworkers.util.*;

import static aggroworkers.util.Constants.rc;

public class Soldier implements RunnableBot {
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> r.getType() == RobotType.SOLDIER || r.getType() == RobotType.SAGE || r.getType() == RobotType.WATCHTOWER);
        if (rc.isMovementReady()) {
            if (closestEnemyAttacker != null) {
                if (rc.isActionReady()) {
                    tryMoveAttackingSquare(closestEnemyAttacker);
                } else {
                    Util.tryKiteFrom(closestEnemyAttacker.location);
                }
            } else {
                RobotInfo closestEnemy = Util.getClosestEnemyRobot();
                if (closestEnemy != null) {
                    Util.tryMove(closestEnemy.location);
                } else {
                    MapLocation loc = Communication.getClosestEnemyChunk();
                    if (loc == null) {
                        Util.tryExplore();
                    } else {
                        Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, loc, 255, 255, 0);
                        Debug.setIndicatorDot(Profile.ATTACKING, Cache.MY_LOCATION, 255, 255, 0);
                        Util.tryMove(loc);
                    }
                }
            }
        }
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
    }

    boolean tryAttackLowHealth() throws GameActionException {
        RobotInfo bestRobot = null;
        int bestHealth = (int)1e9;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (rc.canAttack(robot.location)) {
                if (robot.health < bestHealth) {
                    bestHealth = robot.health;
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

    void tryMoveAttackingSquare(RobotInfo closestEnemy) throws GameActionException {
        double bestScore = 0;
        Direction bestDir = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation loc = rc.getLocation().translate(dx, dy);
                Direction dir = rc.getLocation().directionTo(loc);
                if (dir == Direction.CENTER || rc.canMove(dir)) {
                    int dist = loc.distanceSquaredTo(closestEnemy.location);
                    double distScore = dist <= 13 ? 0.5 + (dist/26.0) : 0;
                    double cooldown = 1.0 + rc.senseRubble(loc)/10.0;
                    double cdScore = 1.0 / cooldown;
                    double score = distScore + 10*cdScore;
                    if (score > bestScore) {
                        bestScore = score;
                        bestDir = dir;
                    }
                }
            }
        }
        if (bestDir != null && bestDir != Direction.CENTER) {
            Util.tryMove(bestDir);
        }
    }
}