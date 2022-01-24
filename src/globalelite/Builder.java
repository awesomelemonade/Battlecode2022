package globalelite;

import battlecode.common.*;
import globalelite.util.*;

import java.util.Comparator;

import static globalelite.util.Constants.*;

public class Builder implements RunnableBot {
    private static int laboratoryModeCounter;

    @Override
    public void init() throws GameActionException {
        laboratoryModeCounter = 0;
    }

    @Override
    public void loop() throws GameActionException {
        if (laboratoryModeCounter > 0 || wantLaboratory()) {
            // Can't exit laboratory mode until we build one.
            laboratoryModeCounter++;
        } else {
            laboratoryModeCounter = 0;
        }
        tryAction();
        tryMove();
        tryAction();
    }

    public static void tryAction() throws GameActionException {
        if (!rc.isActionReady()) return;
        if (tryFinishPrototypes()) return;
        if (laboratoryModeCounter > 0) {
            if (laboratoryModeCounter > 30 || isGoodLaboratorySpot()) {
                if (tryBuildWithReservations(RobotType.LABORATORY)) {
                    laboratoryModeCounter = 0;
                    return;
                }
            }
        }
        if (tryRepair()) {
            return;
        }
    }

    public static void tryMove() throws GameActionException {
        if (!rc.isMovementReady()) return;
        if (tryKiteFromEnemy()) {
            return;
        }
        if (tryMoveToRepair()) {
            return;
        }
        if (laboratoryModeCounter > 0 && tryFindLaboratorySpot()) {
            return;
        }
        if (tryGuard()) {
            return;
        }
        Util.tryExplore(); // should really never happen
    }

    private static MapLocation guardTarget = null;
    private static int lastGuardChangeRound = -1;
    private static MapLocation guardLocation = null;

    public static boolean wantLaboratory() {
        int numLaboratories = Communication.getAliveRobotTypeCount(RobotType.LABORATORY);
        return rc.getTeamLeadAmount(ALLY_TEAM) >= (numLaboratories + 1) * RobotType.LABORATORY.buildCostLead;
    }

    public static boolean nearBorder() {
        int delta = 1;
        return Cache.MY_LOCATION.x <= delta || Cache.MY_LOCATION.y <= delta || Cache.MY_LOCATION.x >= Constants.MAP_WIDTH - 1 - (delta + 1) || Cache.MY_LOCATION.y >= Constants.MAP_HEIGHT - 1 - (delta + 1);
    }

    public static boolean isGoodLaboratorySpot() {
        // Near too many teammates/near enemy archon?
        MapLocation loc = Communication.getClosestCommunicatedAllyArchonLocation();
        if (Cache.ALLY_ROBOTS.length >= 1 || Cache.MY_LOCATION.distanceSquaredTo(loc) < 100) {
            return false;
        }
        int delta = 2;
        // Close to border?
        if (Cache.MY_LOCATION.x <= delta || Cache.MY_LOCATION.y <= delta || Cache.MY_LOCATION.x >= Constants.MAP_WIDTH - 1 - (delta + 1) || Cache.MY_LOCATION.y >= Constants.MAP_HEIGHT - 1 - (delta + 1)) {
            return true;
        }
        // Near enemy?
        loc = Communication.getClosestEnemyChunk();
        if (loc != null && Cache.MY_LOCATION.distanceSquaredTo(loc) < 100) {
            return false;
        }
        return true;
    }

    public static boolean tryFindLaboratorySpot() throws GameActionException {
        // If too crowded, kite from nearest ally
        if (Cache.ALLY_ROBOTS.length >= 5) {
            Util.tryKiteFromGreedy(Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> true).location);
        }
        // If lots of space to kite from enemy chunk, do that
        if (!nearBorder()) {
            MapLocation loc = Communication.getClosestEnemyChunk();
            if (loc != null) {
                Util.tryKiteFromGreedy(loc);
            }
        }
        // Kite from predicted enemy archon location
        MapLocation loc = Communication.getRandomPredictedArchonLocation();
        if (loc == null) return false;
        Util.tryKiteFromGreedy(loc);
        return true;
    }

    public static boolean tryGuard() throws GameActionException {
        if (guardTarget == null || rc.getRoundNum() - lastGuardChangeRound >= 100) {
            guardTarget = getRandomGuardTarget();
            lastGuardChangeRound = rc.getRoundNum();
        }
        if (guardTarget == null) {
            return false;
        }
        if (guardLocation == null || Cache.MY_LOCATION.isWithinDistanceSquared(guardLocation, 2)) {
            guardLocation = getNewGuardLocation();
        }
        if (guardLocation != null) {
            Util.tryMove(guardLocation);
            return true;
        }
        return false;
    }

    public static MapLocation getNewGuardLocation() throws GameActionException {
        for (int i = 10; --i >= 0;) { // Only try 10 times
            int random = (int) (49 * Math.random());
            int dx = random / 7 - 3;
            int dy = random % 7 - 3;
            MapLocation potential = guardTarget.translate(dx, dy);
            if (Util.onTheMap(potential) && !Cache.MY_LOCATION.isWithinDistanceSquared(potential, 2)) {
                return potential;
            }
        }
        return null;
    }

    public static MapLocation getRandomGuardTarget() {
        MapLocation location = null;
        int count = 0;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo ally = Cache.ALLY_ROBOTS[i];
            if (ally.type.isBuilding()) {
                count++;
                if (location == null || Math.random() <= 1.0 / count) {
                    location = ally.location;
                }
            }
        }
        if (location == null) {
            location = Communication.getClosestCommunicatedAllyArchonLocation();
        }
        return location;
    }

    public static boolean tryKiteFromEnemy() throws GameActionException {
        RobotInfo enemy = Util.getClosestEnemyRobot();
        if (enemy == null) {
            return false;
        }
        MapLocation enemyLocation = enemy.location;
        if (enemyLocation == null) return false;
        if (Cache.MY_LOCATION.isWithinDistanceSquared(enemyLocation, 53)) {
            Util.tryKiteFromGreedy(enemyLocation);
            return true;
        }
        return false;
    }

    public static boolean tryMoveToRepair() throws GameActionException {
        MapLocation repairTargetLocation = null;
        int bestDistanceSquared = Integer.MAX_VALUE;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (ROBOT_TYPE.canRepair(robot.type) && robot.health < robot.type.getMaxHealth(robot.level)) {
                int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(robot.location);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    repairTargetLocation = robot.location;
                }
            }
        }
        if (repairTargetLocation == null) return false;
        int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(repairTargetLocation);
        if (distanceSquared <= ROBOT_TYPE.actionRadiusSquared) {
            // Move towards lowest rubble square that is still <= 5 away and adjacent to current location
            int bestRubble = Integer.MAX_VALUE;
            Direction bestDirection = null;
            for (Direction direction : ALL_DIRECTIONS) {
                if (direction == Direction.CENTER || rc.canMove(direction)) {
                    MapLocation location = Cache.MY_LOCATION.add(direction);
                    if (location.isWithinDistanceSquared(repairTargetLocation, 5)) {
                        int rubble = rc.senseRubble(location);
                        if (rubble < bestRubble) {
                            bestRubble = rubble;
                            bestDirection = direction;
                        }
                    }
                }
            }
            if (bestDirection != null && bestDirection != Direction.CENTER) {
                Util.tryMove(bestDirection);
            }
        } else {
            Util.tryMove(repairTargetLocation);
        }
        return true;
    }

    public static boolean tryFinishPrototypes() throws GameActionException {
        MapLocation repairLocation = getBestPrototypeLocation();
        if (repairLocation == null) return false;
        if (rc.canRepair(repairLocation)) {
            rc.repair(repairLocation);
            return true;
        }
        return false;
    }

    public static boolean tryRepair() throws GameActionException {
        MapLocation repairLocation = getBestRepairLocation();
        if (repairLocation == null) return false;
        if (rc.canRepair(repairLocation)) {
            rc.repair(repairLocation);
            return true;
        }
        return false;
    }

    public static MapLocation getBestPrototypeLocation() {
        int bestHealth = Integer.MIN_VALUE;
        MapLocation bestLocation = null;
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(ROBOT_TYPE.actionRadiusSquared, ALLY_TEAM);
        for (int i = nearbyRobots.length; --i >= 0;) {
            RobotInfo ally = nearbyRobots[i];
            MapLocation location = ally.location;
            if (ally.mode == RobotMode.PROTOTYPE && rc.canRepair(location)) {
                int health = ally.health;
                if (health < ally.type.getMaxHealth(ally.level)) {
                    if (health > bestHealth) {
                        bestHealth = health;
                        bestLocation = location;
                    }
                }
            }
        }
        return bestLocation;
    }

    public static MapLocation getBestRepairLocation() {
        int bestHealth = Integer.MAX_VALUE;
        MapLocation bestLocation = null;
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(ROBOT_TYPE.actionRadiusSquared, ALLY_TEAM);
        for (int i = nearbyRobots.length; --i >= 0;) {
            RobotInfo ally = nearbyRobots[i];
            MapLocation location = ally.location;
            if (rc.canRepair(location)) {
                int health = ally.health;
                if (health < ally.type.getMaxHealth(ally.level)) {
                    if (health < bestHealth) {
                        bestHealth = health;
                        bestLocation = location;
                    }
                }
            }
        }
        return bestLocation;
    }

    public static boolean tryBuildWithReservations(RobotType type) throws GameActionException {
        int reservedLead = Communication.getReservedLead();
        int reservedGold = Communication.getReservedGold();

        if (reservedLead != 0 || reservedGold != 0) {
            // There already exists a reservation
            int remainingLead = rc.getTeamLeadAmount(ALLY_TEAM) - type.buildCostLead;
            int remainingGold = rc.getTeamGoldAmount(ALLY_TEAM) - type.buildCostGold;
            if ((remainingLead < reservedLead && remainingLead < rc.getTeamLeadAmount(ALLY_TEAM)) || (remainingGold < reservedGold && remainingGold < rc.getTeamGoldAmount(ALLY_TEAM))) {
                return false;
            } else {
                return tryBuild(type);
            }
        } else {
            // Build if we can, otherwise reserve
            if (tryBuild(type)) {
                return true;
            } else {
                Communication.reserve(type.buildCostGold, type.buildCostLead);
                return false;
            }
        }
    }

    public static boolean tryBuild(RobotType type) throws GameActionException {
        int numLaboratories = Communication.getAliveRobotTypeCount(RobotType.LABORATORY);
        return numLaboratories >= 10 ? tryBuildOnLattice(type) : tryBuildOnLowestRubble(type);
    }

    public static boolean tryBuildOnLowestRubble(RobotType type) throws GameActionException {
        int bestRubble = Integer.MAX_VALUE;
        Direction bestDirection = null;
        for (Direction d: Constants.getAttemptOrder(Util.randomAdjacentDirection())) {
            MapLocation loc = Cache.MY_LOCATION.add(d);
            if (rc.canBuildRobot(type, d)) {
                int rubble = rc.senseRubble(loc);
                if (rubble < bestRubble) {
                    bestRubble = rubble;
                    bestDirection = d;
                }
            }
        }
        if (bestDirection != null) {
            rc.buildRobot(type, bestDirection);
            return true;
        }
        return false;
    }

    public static boolean tryBuildOnLattice(RobotType type) throws GameActionException {
        int bestRubble = Integer.MAX_VALUE;
        Direction bestDirection = null;
        for (Direction d: Constants.getAttemptOrder(Util.randomAdjacentDirection())) {
            MapLocation loc = Cache.MY_LOCATION.add(d);
            if ((loc.x + loc.y) % 2 == 0 && loc.x % 2 == 0) {
                if (rc.canBuildRobot(type, d)) {
                    int rubble = rc.senseRubble(loc);
                    if (rubble < bestRubble) {
                        bestRubble = rubble;
                        bestDirection = d;
                    }
                }
            }
        }
        if (bestDirection != null) {
            rc.buildRobot(type, bestDirection);
            return true;
        }
        return false;
    }
}
