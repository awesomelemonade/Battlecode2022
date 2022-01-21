package opsoldiermicro2;

import battlecode.common.*;
import opsoldiermicro2.util.*;

import static opsoldiermicro2.util.Constants.rc;

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
            tryMove();
        }
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
    }


    private static MapLocation predictedArchonLocation = null;
    public static void tryMoveTowardsEnemyChunkOrExplore() throws GameActionException {
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

    public static void tryMove() throws GameActionException {
        boolean hasEnemyAttackers = LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> Util.isAttacker(r.type));
        MapLocation retreatLocation = getRetreatLocation();
        if (retreatLocation != null) {
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, retreatLocation, 128, 128, 255);
        }
        if (hasEnemyAttackers) {
            if (retreatLocation == null) {
                if (rc.isActionReady()) {
                    tryMoveAttackingSquare();
                } else {
                    RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
                    if (closestEnemyAttacker != null) {
                        Util.tryKiteFrom(closestEnemyAttacker.location);
                    }
                }
            } else {
                tryMoveRetreatingSquare(retreatLocation);
            }
        } else {
            if (retreatLocation == null) {
                // Check for passive enemies
                RobotInfo closestPassiveEnemy = Util.getClosestEnemyRobot();
                if (closestPassiveEnemy == null) {
                    // Go to nearest enemy chunk
                    tryMoveTowardsEnemyChunkOrExplore();
                } else {
                    MapLocation location = closestPassiveEnemy.location;
                    if (!Cache.MY_LOCATION.isWithinDistanceSquared(location, Constants.ROBOT_TYPE.actionRadiusSquared)) {
                        Util.tryMove(closestPassiveEnemy.location);
                    }
                }
            } else {
                // Retreat to retreatLocation
                if (Cache.MY_LOCATION.isWithinDistanceSquared(retreatLocation, 20)) {
                    // do nothing
                } else {
                    Util.tryMove(retreatLocation);
                }
            }
        }
    }

    public static void tryMoveRetreatingSquare(MapLocation archonLocation) throws GameActionException {
        double bestScore = -Double.MAX_VALUE;
        Direction bestDirection = null;
        // Loop through all neighboring squares that are closer to the archonLocation
        // Maximize ourFrequency / enemyFrequency, given that enemyFrequency > min turns we spend there
        int currentDist2 = Cache.MY_LOCATION.distanceSquaredTo(archonLocation);
        double cooldown = 1.0 + rc.senseRubble(Cache.MY_LOCATION) / 10.0;
        double cooldownAfterMove = (rc.getMovementCooldownTurns() - Constants.ROBOT_TYPE.movementCooldown * cooldown);
        int turnsStuck = (int) Math.ceil((cooldownAfterMove - 9.999999) / 10.0);
        Generated13.debug_execute(archonLocation);
        for (Direction direction : Constants.ALL_DIRECTIONS) {
            MapLocation location = Cache.MY_LOCATION.add(direction);
            if (rc.onTheMap(location)) {
                int distance2 = location.distanceSquaredTo(archonLocation);
                if (distance2 > currentDist2) {
                    continue;
                }
                double ourFrequency = 3.0 / (1.0 + rc.senseRubble(location) / 10.0);
                if (!rc.canMove(direction) && direction != Direction.CENTER) {
                    ourFrequency /= 3; // Penalize 3 turns
                }
                double totalEnemyDamageFrequency = 0; // amount of damage per turn
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
                        if (robot.location.isWithinDistanceSquared(location, 25)) {
                            totalEnemyDamageFrequency += robot.type.getDamage(robot.level) / (1.0 + rc.senseRubble(robot.location) / 10.0);
                        }
                    }
                }
                double amountOfDamageSustained = totalEnemyDamageFrequency * turnsStuck;
                double score;
                if (amountOfDamageSustained <= 0.05) {
                    // No enemies
                    score = 10000.0 + Generated13.;
                } else {
                    if (amountOfDamageSustained >= rc.getHealth()) {
                        // Terrible score - it's going to die
                        score = ourFrequency / totalEnemyDamageFrequency - 10000.0;
                    } else {
                        // it's not going to die - let's just treat it normally
                        score = ourFrequency / totalEnemyDamageFrequency;
                    }
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestDirection = direction;
                }
            }
        }
        if (bestDirection != null && rc.canMove(bestDirection)) {
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, Cache.MY_LOCATION.add(bestDirection), 128, 0, 255);
            Util.tryMove(bestDirection);
        }
    }

    public static void tryMoveAttackingSquare() throws GameActionException {
        double bestScore = -Double.MAX_VALUE;
        Direction bestDir = null;
        //StringBuilder builder = new StringBuilder("Score " + Cache.MY_LOCATION + ": ");
        for (Direction dir : Constants.ALL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(dir);
            if (rc.onTheMap(loc)) {
                double ourFrequency = 1.0 / (1.0 + rc.senseRubble(loc) / 10.0);
                if (!rc.canMove(dir) && dir != Direction.CENTER) {
                    ourFrequency /= 3; // Penalize 3 turns
                }
                double totalEnemyFrequency = 0; // TODO: Instead - shouldn't it be damage per turn instead of attacks per turn
                double closestDistance = 1e9;
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
                double ratio = ourFrequency / totalEnemyFrequency;
                if (totalEnemyFrequency != 0 && ratio >= 1.0) {
                    score = ratio + 1000;
                } else if (totalEnemyFrequency == 0) {
                    score = ourFrequency;
                } else {
                    // this should never be the best score
                    score = ourFrequency - 1000;
                }
                //builder.append(dir + "=" + score + " - " + ourFrequency + " - " + totalEnemyFrequency + ", ");
                if (score > bestScore) {
                    bestScore = score;
                    bestDir = dir;
                }
            }
        }
        //Debug.println(builder.toString());
        //Debug.println(bestScore + " - " + bestDir);
        //Debug.println(Arrays.toString(Arrays.stream(Cache.ENEMY_ROBOTS).filter(x -> Util.isAttacker(x.type)).map(x -> x.location).toArray()));
        if (bestDir == null) {
            // wtf?
            throw new IllegalStateException("are all scores -infinity???");
        } else {
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, Cache.MY_LOCATION.add(bestDir), 255, 0, 0);
            Util.tryMove(bestDir);
        }
    }

    public static MapLocation getRetreatLocation() throws GameActionException {
        if (rc.getHealth() >= rc.getType().getMaxHealth(rc.getLevel())) {
            return null;
        }
        int healthThreshold = (int) Math.round(Math.max(15.0, Math.min(30.0, 40.0 - 0.1 * Communication.getSoldierCombinedHealth())));
        if (rc.getHealth() <= healthThreshold) {
            return idealThenNearestVisible();
        }
        MapLocation location = nearestVisibleThenIdeal();
        return location != null && Cache.MY_LOCATION.isWithinDistanceSquared(location, 34) ? location : null;
    }

    private static MapLocation idealThenNearestVisible() {
        MapLocation location = getIdealAllyArchonForHeal();
        return location == null ? getNearestVisibleAllyArchonForHeal() : location;
    }

    private static MapLocation nearestVisibleThenIdeal() {
        MapLocation location = getNearestVisibleAllyArchonForHeal();
        return location == null ? getIdealAllyArchonForHeal() : location;
    }

    private static MapLocation getIdealAllyArchonForHeal() {
        double bestScore = Double.MAX_VALUE;
        MapLocation bestLocation = null;
        for (int i = Communication.archonLocations.length; --i >= 0; ) {
            MapLocation loc = Communication.archonLocations[i];
            if (loc == null) continue;
            if (Communication.archonPortable[i]) continue;
            double health = Communication.archonRepairAmounts[i];
            double score = health / 3.0 + 2.0 * Math.sqrt(loc.distanceSquaredTo(Cache.MY_LOCATION));
            if (score < bestScore) {
                bestScore = score;
                bestLocation = loc;
            }
        }
        return bestLocation;
    }

    public static MapLocation getNearestVisibleAllyArchonForHeal() {
        int bestDistanceSquared = Integer.MAX_VALUE;
        MapLocation bestMapLocation = null;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (robot.type == RobotType.ARCHON && robot.mode == RobotMode.TURRET) {
                MapLocation location = robot.location;
                int distanceSquared = location.distanceSquaredTo(Cache.MY_LOCATION);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestMapLocation = location;
                }
            }
        }
        return bestMapLocation;
    }
}
