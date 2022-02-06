package opsoldiermicro;

import battlecode.common.*;
import opsoldiermicro.util.*;

import static opsoldiermicro.util.Constants.rc;

public class Soldier implements RunnableBot {
    private static final int RETREATING_HEALTH_THRESHOLD = 15;
    private static boolean retreating = false;

    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getHealth() <= RETREATING_HEALTH_THRESHOLD) {
            retreating = true;
        } else if (rc.getHealth() >= rc.getType().getMaxHealth(rc.getLevel())) {
            retreating = false;
        }
        if (retreating) {
            Communication.setPassive();
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
        boolean canCurrentlyAttack = rc.isActionReady();
        double bestScore = -Double.MAX_VALUE;
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
            // if we can't currently attack - go away from the enemy if there is less rubble, otherwise stand still
            //      only consider squares where totalEnemyFrequency < center square totalEnemyFrequency
            //      then tiebreak by going to the square with the least rubble (maximize ourFrequency)
            //      then tiebreak by minimizing distance to closestEnemy
            // if we can currently attack - step into to attack (presumably, there wasn't a valid attack premove)
            //      maximize ourFrequency / totalEnemyFrequency, given that we are withinAttackRadius and ratio >= 1
            //      maximize ourFrequency / totalEnemyFrequency, given that we are not withinAttackRadius
            //      ^ this second one is always possible because center is guaranteed to be not within attack radius (or else isActionReady() would return false)
            for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
                if (Util.isAttacker(robot.type)) {
                    boolean withinAttackRadius = robot.location.isWithinDistanceSquared(loc, robot.type.actionRadiusSquared);
                    if (withinAttackRadius) {
                        ableToAttackEnemy = true;
                    }
                    if (withinAttackRadius) {
                        totalEnemyFrequency += 1.0 / (1.0 + rc.senseRubble(robot.location) / 10.0);
                    }
                    double dist = robot.location.distanceSquaredTo(loc);
                    if (dist < closestDistance) {
                        closestDistance = dist;
                    }
                }
            }
            double score;
            if (canCurrentlyAttack) {
                double ratio = ourFrequency / totalEnemyFrequency;
                // TODO: Change 0.75 ratio?
                if (ableToAttackEnemy && ratio >= 1.0) {
                    score = ratio + 1000;
                } else if (!ableToAttackEnemy) {
                    score = ourFrequency;
                } else {
                    // this should never be the best score
                    score = ourFrequency - 1000;
                }
            } else {
                score = -1000000.0 * totalEnemyFrequency + 10000 * ourFrequency + closestDistance;
            }
            //builder.append(dir + "=" + score + " - " + ourFrequency + " - " + totalEnemyFrequency + ", ");
            if (score > bestScore) {
                bestScore = score;
                bestDir = dir;
            }
        }
        //Debug.println(builder.toString());
        //Debug.println(bestScore + " - " + bestDir);
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
        if (!Cache.MY_LOCATION.isWithinDistanceSquared(location, rc.getType().actionRadiusSquared)) {
            Util.tryMove(location);
        }
    }


    public static boolean tryRetreat() throws GameActionException {
        if (!retreating) {
            return false;
        }
        MapLocation loc = MapInfo.getClosestAllyArchonLocation();
        if (loc == null) return false;

        int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
        if (dist <= 10) {
            return false;
        } else {
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, loc, 128, 128, 255);
            Util.tryMove(loc);
            return true;
        }
    }
}
