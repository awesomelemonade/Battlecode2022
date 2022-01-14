package soldiermicro;

import battlecode.common.*;
import soldiermicro.util.*;

import java.util.Comparator;
import java.util.Optional;

import static soldiermicro.util.Constants.rc;

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


    private static MapLocation predictedArchonLocation = null;
    public static void tryMove() throws GameActionException {
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> r.getType() == RobotType.SOLDIER || r.getType() == RobotType.SAGE || r.getType() == RobotType.WATCHTOWER);
        if (tryRetreat()) {
            return;
        }
        if (tryAttackerMicro()) {
            return;
        }

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

    public static boolean tryAttackerMicro() throws GameActionException {
        double bestScore = -Double.MAX_VALUE;
        Direction bestDir = null;
        for (Direction dir : Constants.ALL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(dir);
            if (!rc.canMove(dir) && dir != Direction.CENTER) continue;
            double ourFrequency = 1.0 / (1.0 + rc.senseRubble(loc) / 10.0);
            double totalEnemyFrequency = 0;
            double closestDistance = 1e9;
            for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
                if (Util.isAttacker(robot.type)) {
                    if (robot.location.isWithinDistanceSquared(loc, robot.type.actionRadiusSquared)) {
                        totalEnemyFrequency += 1.0 / (1.0 + rc.senseRubble(robot.location) / 10.0);
                    }
                    double dist = robot.location.distanceSquaredTo(loc);
                    if (dist < closestDistance) {
                        closestDistance = dist;
                    }
                }
            }
            double score;
            if (totalEnemyFrequency == 0) {
                score = 0;
            } else {
                if (ourFrequency > totalEnemyFrequency) {
                    score = 10000 * (ourFrequency / totalEnemyFrequency) + closestDistance;
                } else {
                    score = 0;
                }
            }
            Debug.println(dir + ": " + score);
            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }
        if (bestScore <= 10000) bestDir = null;
            if (bestDir != null && LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> Util.isAttacker(r.type))) {
            Debug.setIndicatorLine(Cache.MY_LOCATION, Cache.MY_LOCATION.add(bestDir), 255, 0,0);
            Util.tryMove(bestDir);
            return true;
        } else {
            return false;
        }
    }

    public static void tryMoveAttackingSquare(MapLocation location, int range) throws GameActionException {
        double bestScore = 0;
        Direction bestDir = null;
        double theircd = 1.0 + rc.senseRubble(location)/10.0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation loc = rc.getLocation().translate(dx, dy);
                Direction dir = rc.getLocation().directionTo(loc);
                if (dir == Direction.CENTER || rc.canMove(dir)) {
                    int dist = loc.distanceSquaredTo(location);
                    double distScore = 0.5 + dist / (2.0 * range);
                    if (dist > range) distScore = 0.2/(range-19) + 4.0/(19-range);
                    double cdScore = theircd / (1.0 + rc.senseRubble(loc)/10.0);
                    double score = distScore + 10 * cdScore;
                    if (score > bestScore) {
                        bestScore = score;
                        bestDir = dir;
                    }
                }
            }
        }
        if (bestDir != null && bestDir != Direction.CENTER) {
            Debug.setIndicatorLine(Cache.MY_LOCATION, Cache.MY_LOCATION.add(bestDir), 0, 255,0);
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
