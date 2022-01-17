package combinedsoldierhealth;

import battlecode.common.*;
import combinedsoldierhealth.util.*;

import static combinedsoldierhealth.util.Constants.ALL_DIRECTIONS;
import static combinedsoldierhealth.util.Constants.rc;

public class Soldier implements RunnableBot {
    private static RobotInfo closestEnemyAttacker;
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        Communication.setSoldierHealthInfo(rc.getHealth());
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
        if (closestEnemyAttacker != null) {
            if (rc.isActionReady()) {
                tryMoveAttackingSquare(closestEnemyAttacker.location, 13);
            } else {
                Debug.setIndicatorLine(Cache.MY_LOCATION, closestEnemyAttacker.location, 0, 0, 0);
                Util.tryKiteFrom(closestEnemyAttacker.location);
            }
        } else {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            Debug.setIndicatorDot(Cache.MY_LOCATION, 255, 128, 0);
            Debug.setIndicatorString(closestEnemy == null ? "null" : closestEnemy.toString());
            if (closestEnemy != null) {
                Debug.setIndicatorLine(Cache.MY_LOCATION, closestEnemy.location, 255, 128, 0);
                tryMoveAttackingSquare(closestEnemy.location, 13);
            } else {
                MapLocation location = Communication.getClosestEnemyChunkButNotAdjacentToChunkCenter();
                if (location == null) {
                    if (predictedArchonLocation == null || Communication.getChunkInfo(predictedArchonLocation) != Communication.CHUNK_INFO_ENEMY_PREDICTED) {
                        predictedArchonLocation = Communication.getRandomPredictedArchonLocation();
                    }
                    location = predictedArchonLocation;
                }
                if (location == null) {
                    Debug.setIndicatorDot(Cache.MY_LOCATION, 255, 255, 255);
                    Util.tryExplore();
                } else {
                    Debug.setIndicatorLine(Cache.MY_LOCATION, location, 255, 255, 0);
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
        Debug.setIndicatorLine(Cache.MY_LOCATION, location, 255, 0, 0);
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
                    double score = distScore + 5 * cdScore;
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

    public static void tryMoveRetreatingSquare(MapLocation archonLocation, MapLocation closestEnemy) throws GameActionException {
        Debug.setIndicatorLine(Cache.MY_LOCATION, closestEnemy, 0, 0, 255);
        Debug.setIndicatorLine(Cache.MY_LOCATION, archonLocation, 0, 255, 255);
        // Pick a direction that gets us closer to archon, but further from attacker
        double bestScore = -Double.MAX_VALUE;
        double bestScore2 = -Double.MAX_VALUE;
        Direction bestDir = null;
        int currentDistanceToArchon = Cache.MY_LOCATION.distanceSquaredTo(archonLocation);
        for (Direction direction : Constants.ORDINAL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(direction);
            if (rc.onTheMap(loc) && (loc.distanceSquaredTo(archonLocation) < Math.max(currentDistanceToArchon, RobotType.ARCHON.actionRadiusSquared + 1))) {
                int distanceToEnemy = loc.distanceSquaredTo(closestEnemy);
                double distScore = distanceToEnemy / 20.0;
                double cooldown = 1.0 + rc.senseRubble(loc) / 10.0;
                double cdScore = 1.0 / cooldown;
                double score = distScore + 5 * cdScore;
                double score2 = rc.canMove(direction) ? 1 : 0;
                if (score > bestScore || (score == bestScore && score2 > bestScore2)) {
                    bestScore = score;
                    bestDir = direction;
                    bestScore2 = score2;
                }
            }
        }
        if (bestDir != null) {
            Debug.setIndicatorLine(Cache.MY_LOCATION, Cache.MY_LOCATION.add(bestDir), 0, 255, 0);
        }
        if (bestDir != null && rc.canMove(bestDir)) {
            Util.tryMove(bestDir);
        } else {
            tryMoveRetreatingSquare2(archonLocation, closestEnemy);
        }
    }

    public static void tryMoveRetreatingSquare2(MapLocation archonLocation, MapLocation closestEnemy) throws GameActionException {
        Debug.setIndicatorDot(Cache.MY_LOCATION, 0, 255, 255);
        // Pick a direction that gets us closer to archon, but further from attacker
        double bestScore = -Double.MAX_VALUE;
        Direction bestDir = null;
        int currentDistanceToArchon = Cache.MY_LOCATION.distanceSquaredTo(archonLocation);
        for (Direction direction : ALL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(direction);
            if (direction == Direction.CENTER || (rc.canMove(direction) && loc.distanceSquaredTo(archonLocation) < Math.max(currentDistanceToArchon, RobotType.ARCHON.actionRadiusSquared + 1))) {
                int distanceToEnemy = loc.distanceSquaredTo(closestEnemy);
                double distScore = distanceToEnemy / 20.0;
                double cooldown = 1.0 + rc.senseRubble(loc) / 10.0;
                double cdScore = 1.0 / cooldown;
                double score = distScore + 5 * cdScore;
                if (score > bestScore) {
                    bestScore = score;
                    bestDir = direction;
                }
            }
        }
        if (bestDir != null && bestDir != Direction.CENTER) {
            Util.tryMove(bestDir);
        }
    }

    public static boolean tryRetreat() throws GameActionException {
        if (rc.getHealth() >= rc.getType().getMaxHealth(rc.getLevel())) return false;
        MapLocation bestLoc = null;
        double bestScore = 1e9;
        if (Communication.archonLocations != null) {
            for (int i = Communication.archonLocations.length; --i >= 0; ) {
                MapLocation loc = Communication.archonLocations[i];
                if (loc == null) continue;
                if (Communication.archonPortable[i]) continue;
                double health = Communication.archonRepairAmounts[i];
                double score = health/3 + 2*Math.sqrt(loc.distanceSquaredTo(Cache.MY_LOCATION));
                if (score < bestScore) {
                    bestScore = score;
                    bestLoc = loc;
                }
            }
        }
        if (bestLoc == null) {
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                RobotInfo robot = Cache.ALLY_ROBOTS[i];
                if (robot.type == RobotType.ARCHON) {
                    MapLocation location = robot.location;
                    int distanceSquared = location.distanceSquaredTo(Cache.MY_LOCATION);
                    if (distanceSquared < bestScore) {
                        bestScore = distanceSquared;
                        bestLoc = location;
                    }
                }
            }
        }
        if (bestLoc == null) {
            return false;
        }

        // f(2 soldiers) = 30, f(5 soldiers) = 15
        int healthThreshold = (int) Math.round(Math.max(15.0, Math.min(30.0, 40.0 - 0.1 * Communication.getSoldierCombinedHealth())));
        int dist = bestLoc.distanceSquaredTo(Cache.MY_LOCATION);
        if (dist <= 10) {
            if (closestEnemyAttacker != null) {
                tryMoveRetreatingSquare(bestLoc, closestEnemyAttacker.location);
            }
            return true;
        } else if (rc.getHealth() <= healthThreshold || dist <= RobotType.ARCHON.actionRadiusSquared) {
            if (closestEnemyAttacker != null) {
                tryMoveRetreatingSquare(bestLoc, closestEnemyAttacker.location);
                return true;
            }
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, bestLoc, 128, 128, 255);
            Util.tryMove(bestLoc);
            return true;
        }
        return false;
    }
}
