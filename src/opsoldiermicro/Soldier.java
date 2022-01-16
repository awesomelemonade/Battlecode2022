package opsoldiermicro;

import battlecode.common.*;
import opsoldiermicro.util.*;

import java.util.Arrays;

import static opsoldiermicro.util.Constants.rc;

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
        if (tryRetreat()) {
            return;
        }
        if (tryAttackerMicro()) {
            return;
        }
        RobotInfo closestEnemy = Util.getClosestEnemyRobot(); // Assumption is that there are only passive enemies left
        if (closestEnemy != null) {
            tryMoveToAttackPassive(closestEnemy.location);
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
            lastTurnAttacked = rc.getRoundNum();
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

    public static boolean tryAttackerMicro() throws GameActionException {
        boolean useAttackerMicro = LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> Util.isAttacker(r.type));
        if (!useAttackerMicro) {
            return false;
        }
        double bestScore = -Double.MAX_VALUE;
        double bestScore2 = -Double.MAX_VALUE;
        Direction bestDir = null;
        //StringBuilder builder = new StringBuilder("Score " + Cache.MY_LOCATION + ": ");
        // TODO: Penalize any enemies near enemy archons?
        // TODO: Buff ourselves near our own archon?
        for (Direction dir : Constants.ALL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(dir);
            if (!rc.canMove(dir) && dir != Direction.CENTER) continue;
            double ourFrequency = 1.0 / (1.0 + rc.senseRubble(loc) / 10.0);
            double totalEnemyFrequency = 0;
            double closestDistance = 1e9;
            boolean ableToAttackEnemy = false;
            for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
                if (Util.isAttacker(robot.type)) {
                    boolean withinAttackRadius = robot.location.isWithinDistanceSquared(loc, robot.type.actionRadiusSquared);
                    if (withinAttackRadius) {
                        ableToAttackEnemy = true;
                    }
                    if (withinAttackRadius ||
                            (rc.senseRubble(robot.location) <= rc.senseRubble(loc) &&
                                    robot.location.isWithinDistanceSquared(loc, 25))) {
                        totalEnemyFrequency += 1.0 / (1.0 + rc.senseRubble(robot.location) / 10.0);
                    }
                    double dist = robot.location.distanceSquaredTo(loc);
                    if (dist < closestDistance) {
                        closestDistance = dist;
                    }
                }
            }
            // TODO: score2 should minimize distance to border
            double score2 = closestDistance; // Minimize our distance to enemy (don't want to move out of attacking range)
            double score;
            if (!ableToAttackEnemy) {
                score = ourFrequency; // Minimize rubble
            } else {
                if (ourFrequency >= totalEnemyFrequency) {
                    score = 100000.0 * (ourFrequency / totalEnemyFrequency);
                } else {
                    score = -100000.0 * (totalEnemyFrequency / ourFrequency);
                }
            }
            //builder.append(dir + "=" + score + " - " + ourFrequency + " - " + totalEnemyFrequency + ", ");
            if (score > bestScore || score == bestScore && score2 > bestScore2) {
                bestScore = score;
                bestScore2 = score2;
                bestDir = dir;
            }
        }
        //Debug.println(builder.toString());
        //Debug.println(bestScore + " - " + bestScore2 + " - " + bestDir);
        //Debug.println(Arrays.toString(Arrays.stream(Cache.ENEMY_ROBOTS).filter(x -> Util.isAttacker(x.type)).map(x -> x.location).toArray()));
        if (bestDir == null) {
            // wtf?
            throw new IllegalStateException("are all scores -infinity???");
        } else {
            Debug.setIndicatorLine(Cache.MY_LOCATION, Cache.MY_LOCATION.add(bestDir), 255, 0, 0);
            Util.tryMove(bestDir);
            return true;
        }
    }

    public static void tryMoveToAttackPassive(MapLocation location) throws GameActionException {
        // prioritize being able to attack the location, then prioritize low rubble
        double bestScore = -Double.MAX_VALUE;
        Direction bestDir = null;
        for (Direction direction : Constants.ALL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(direction);
            if (direction == Direction.CENTER || rc.canMove(direction)) {
                boolean isAbleToAttack = loc.isWithinDistanceSquared(location, RobotType.SOLDIER.actionRadiusSquared);
                int rubble = rc.senseRubble(loc);
                double score = (isAbleToAttack ? 100000 : 0) - rubble;
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
        if (rc.getHealth() == rc.getType().getMaxHealth(rc.getLevel())) return false;

        MapLocation loc = MapInfo.getClosestAllyArchonLocation();
        if (loc == null) return false;

        int healthThreshold = 15;
        int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
        if (dist <= 10) {
            Communication.setPassive();
            return false;
        } else if (rc.getHealth() <= healthThreshold || dist <= RobotType.ARCHON.actionRadiusSquared) {
            Communication.setPassive();
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, loc, 128, 128, 255);
            Util.tryMove(loc);
            return true;
        }
        return false;
    }
}
