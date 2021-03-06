package realhealingcomms;

import battlecode.common.*;
import realhealingcomms.util.*;

import java.util.Comparator;
import java.util.Optional;

import static realhealingcomms.util.Constants.rc;

public class Soldier implements RunnableBot {
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
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


    private static MapLocation predictedArchonLocation = null;
    public static void tryMove() throws GameActionException {
        if (tryRetreat()) {
            return;
        }
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (closestEnemyAttacker != null) {
            if (rc.isActionReady()) {
                tryMoveAttackingSquare(closestEnemyAttacker.location, 13);
            } else {
                Util.tryKiteFrom(closestEnemyAttacker.location);
            }
        } else {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            if (closestEnemy != null) {
                tryMoveAttackingSquare(closestEnemy.location, 13);
            } else {
                MapLocation location = Communication.getClosestEnemyChunk();
                if (location == null) {
                    if (predictedArchonLocation == null || Communication.getChunkInfo(predictedArchonLocation) != Communication.CHUNK_INFO_ENEMY_PREDICTED) {
                        predictedArchonLocation = Communication.getRandomPredictedArchonLocation();
                    }
                    location = predictedArchonLocation;
                }
                if (location == null) {
                    Util.tryExplore();
                } else {
                    Debug.setIndicatorDot(Profile.ATTACKING, Cache.MY_LOCATION, 255, 255, 0);
                    Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, location, 255, 255, 0);
                    Util.tryMove(location);
                }
            }
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

    // Prioritize soldiers = sages = watchtowers > miners > builders > archons > laboratories
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

    public static void tryMoveAttackingSquare(MapLocation location, int range) throws GameActionException {
        double bestScore = 0;
        Direction bestDir = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation loc = rc.getLocation().translate(dx, dy);
                Direction dir = rc.getLocation().directionTo(loc);
                if (dir == Direction.CENTER || rc.canMove(dir)) {
                    int dist = loc.distanceSquaredTo(location);
                    double distScore = 0.5 + dist / (2.0 * range);
                    if (dist > range) distScore = 0.75 - dist / (2.0 * range);
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
        if (rc.getHealth() == rc.getType().getMaxHealth(rc.getLevel())) return false;

        MapLocation bestLoc = null;
        double bestScore = 1e9;
        for (int i = MapInfo.CURRENT_ARCHON_LOCATIONS.length; --i >= 0; ) {
            MapLocation loc = MapInfo.CURRENT_ARCHON_LOCATIONS[i];
            if (loc == null) continue;
            double health = MapInfo.ARCHON_REPAIR_AMOUNTS[i];
            double score = health/3 + 2*Math.sqrt(loc.distanceSquaredTo(Cache.MY_LOCATION));
            if (score < bestScore) {
                bestScore = score;
                bestLoc = loc;
            }
        }
        if (bestLoc == null) return false;

        int healthThreshold = 15;
        int dist = bestLoc.distanceSquaredTo(Cache.MY_LOCATION);
        if (dist <= 10) {
            Communication.setPassive();
            return false;
        } else if (rc.getHealth() <= healthThreshold || dist <= RobotType.ARCHON.actionRadiusSquared) {
            Communication.setPassive();
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, bestLoc, 128, 128, 255);
            Util.tryMove(bestLoc);
            return true;
        }
        return false;
    }
}
