package newbuildorder2;

import battlecode.common.*;
import newbuildorder2.util.*;

import java.util.Comparator;
import java.util.Optional;

import static newbuildorder2.util.Constants.rc;

public class Soldier implements RunnableBot {
    private static int lastTurnAttacked = 0;
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        int threshold = rc.getRoundNum() > 1500 ? 100 : (rc.getRoundNum() > 1000 ? 250 : 500);
        if (Cache.TURN_COUNT > 10 && rc.getRoundNum() - lastTurnAttacked >= threshold) {
            Communication.setPassive();
            // we're counted as a passive soldier - otherwise we're an active soldier
        }
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
        if (rc.isMovementReady()) {
            tryMove();
        }
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
    }


    public static void tryMove() throws GameActionException {
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> r.getType() == RobotType.SOLDIER || r.getType() == RobotType.SAGE || r.getType() == RobotType.WATCHTOWER);
        if (tryRetreat()) {
            return;
        }
        if (closestEnemyAttacker != null) {
            if (rc.isActionReady()) {
                tryMoveAttackingSquare(closestEnemyAttacker.location, 13);
            } else {
                Util.tryKiteFrom(closestEnemyAttacker.location);
            }
        } else {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            if (closestEnemy != null) {
                tryMoveAttackingSquare(closestEnemy.location, 10); // Don't want to lose track
            } else {
                MapLocation loc = Communication.getClosestEnemyChunk();
                if (loc == null) {
                    Util.tryExplore();
                } else {
                    Util.tryMove(loc);
                }
            }
        }
    }
    public static boolean tryAttackLowHealth() throws GameActionException {
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
            lastTurnAttacked = rc.getRoundNum();
            return true;
        }
        return false;
    }

    public static void tryMoveAttackingSquare(MapLocation location, int range) throws GameActionException {
        double bestScore = 0;
        Direction bestDir = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation loc = rc.getLocation().translate(dx, dy);
                Direction dir = rc.getLocation().directionTo(loc);
                if (dir == Direction.CENTER || rc.canMove(dir)) {
                    int dist = loc.distanceSquaredTo(location);
                    double distScore = dist <= range ? 0.5 + (dist / (2.0 * range)) : 0;
                    double cooldown = 1.0 + rc.senseRubble(loc) / 10.0;
                    double cdScore = 1.0 / cooldown;
                    double score = distScore + 10 * cdScore;
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


    public static boolean tryRetreat() throws GameActionException {
        RobotInfo[] allies = Cache.ALLY_ROBOTS;
        MapLocation bestArchonLocation = LambdaUtil.arraysStreamMin(allies, r -> r.getType() == RobotType.ARCHON, Comparator.comparingInt(r -> r.getLocation().distanceSquaredTo(rc.getLocation()))).map(r -> r.getLocation()).orElse(null);
        // No archon nearby
        if (bestArchonLocation == null) {
            return false;
        }
        int healthThreshold = 10;
        if (rc.getHealth() < healthThreshold) {
            Util.tryMove(bestArchonLocation);
            return true;
        }
        else if (rc.getHealth() >= healthThreshold && rc.getHealth() < rc.getType().health && bestArchonLocation.distanceSquaredTo(rc.getLocation()) <= RobotType.ARCHON.actionRadiusSquared && Cache.ENEMY_ROBOTS.length == 0) {
            return true;
        }
        return false;
    }
}
