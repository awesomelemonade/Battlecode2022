package faceit7;

import battlecode.common.*;
import faceit7.util.*;

import static faceit7.util.Constants.*;

public class Builder implements RunnableBot {
    private static RobotInfo[] ALLY_ROBOTS_ACTION_RADIUS = new RobotInfo[0];
    private static MapLocation justBuiltLocation = null;
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        justBuiltLocation = null;
        if (rc.isActionReady()) {
            ALLY_ROBOTS_ACTION_RADIUS = rc.senseNearbyRobots(ROBOT_TYPE.actionRadiusSquared, ALLY_TEAM);
            tryAction();
        }
        if (rc.isMovementReady()) {
            tryMove();
        }
        if (rc.isActionReady()) {
            ALLY_ROBOTS_ACTION_RADIUS = rc.senseNearbyRobots(ROBOT_TYPE.actionRadiusSquared, ALLY_TEAM);
            tryAction();
        }
    }

    public static void tryAction() throws GameActionException {
        RobotInfo repairTarget = getBestRepairTarget();
        if (repairTarget != null && repairTarget.mode == RobotMode.PROTOTYPE) {
            tryRepair(repairTarget.location);
            return;
        }
        int numLaboratories = Communication.getAliveRobotTypeCount(RobotType.LABORATORY);
        if (rc.getTeamLeadAmount(ALLY_TEAM) >= Math.min(500, numLaboratories * 150)) {
            if (tryBuildWithReservations(RobotType.LABORATORY)) {
                return;
            }
        }
        if (repairTarget != null) {
            tryRepair(repairTarget.location);
            return;
        }
    }

    public static void tryMove() throws GameActionException {
        if (tryKiteFromVisibleAttacker()) {
            return;
        }
        MapLocation repairTarget = getTryMoveToRepairTarget();
        if (repairTarget != null) {
            if (tryMoveToRepair(repairTarget)) {
                return;
            }
        }
        if (tryKiteFromAllEnemies()) {
            return;
        }
        if (tryGuard()) {
            return;
        }
        Util.tryExplore();
    }

    public static MapLocation findBestFarmingLocation() {
        // Has to be far away from closest enemy miner (and other) chunks (solidly ally?)
        return null;
    }

    private static MapLocation guardTarget = null;
    private static int lastGuardChangeRound = -1;
    private static MapLocation guardLocation = null;

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
            int dx = Random.rand256() % 7 - 3;
            int dy = Random.rand256() % 7 - 3;
            MapLocation potential = guardTarget.translate(dx, dy);
            if (Util.onTheMap(potential) && !Cache.MY_LOCATION.isAdjacentTo(potential)) {
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


    public static boolean tryKiteFromVisibleAttacker() throws GameActionException {
        RobotInfo enemy = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (enemy == null) {
            return false;
        } else {
            Util.tryKiteFromGreedy(enemy.location);
            return true;
        }
    }
    public static boolean tryKiteFromAllEnemies() throws GameActionException {
        RobotInfo enemy = Util.getClosestEnemyRobot();
        MapLocation enemyLocation = enemy == null ? Communication.getClosestEnemyChunk() : enemy.location;
        if (enemyLocation == null) return false;
        if (Cache.MY_LOCATION.isWithinDistanceSquared(enemyLocation, 53)) {
            Util.tryKiteFromGreedy(enemyLocation);
            return true;
        }
        return false;
    }

    public static boolean tryMoveToRepair(MapLocation repairTargetLocation) throws GameActionException {
        MapLocation[] potentialLocations = rc.getAllLocationsWithinRadiusSquared(repairTargetLocation, 5);
        MapLocation bestLocation = null;
        int bestRubble = Integer.MAX_VALUE;
        int bestDistanceSquared = Integer.MAX_VALUE;
        for (int i = potentialLocations.length; --i >= 0;) {
            MapLocation location = potentialLocations[i];
            if (location.equals(Cache.MY_LOCATION) || rc.canSenseLocation(location) && !rc.isLocationOccupied(location)) {
                int rubble = rc.senseRubble(location);
                int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(location);
                if (rubble < bestRubble || (rubble == bestRubble && distanceSquared < bestDistanceSquared)) {
                    bestRubble = rubble;
                    bestDistanceSquared = distanceSquared;
                    bestLocation = location;
                }
            }
        }
        MapLocation target = bestLocation == null ? repairTargetLocation : bestLocation;
        Util.tryMove(target);
        return true;
    }

    public static MapLocation getTryMoveToRepairTarget() throws GameActionException {
        if (justBuiltLocation != null) {
            return justBuiltLocation;
        }
        RobotInfo repairTarget = null;
        int bestDistanceSquared = Integer.MAX_VALUE;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (ROBOT_TYPE.canRepair(robot.type) && robot.health < robot.type.getMaxHealth(robot.level)) {
                int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(robot.location);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    repairTarget = robot;
                }
            }
        }
        return repairTarget == null ? null : repairTarget.location;
    }

    public static boolean tryRepair(MapLocation location) throws GameActionException {
        if (rc.canRepair(location)) {
            rc.repair(location);
            return true;
        }
        return false;
    }

    // Prioritizes prototype > turret, low health > high health
    public static RobotInfo getBestRepairTarget() {
        int bestScore = Integer.MAX_VALUE;
        RobotInfo bestTarget = null;
        for (int i = ALLY_ROBOTS_ACTION_RADIUS.length; --i >= 0;) {
            RobotInfo ally = ALLY_ROBOTS_ACTION_RADIUS[i];
            if (rc.canRepair(ally.location)) {
                int health = ally.health;
                if (health < ally.type.getMaxHealth(ally.level)) {
                    int score = health - (ally.mode == RobotMode.PROTOTYPE ? 10000 : 0);
                    if (score < bestScore) {
                        bestScore = score;
                        bestTarget = ally;
                    }
                }
            }
        }
        return bestTarget;
    }

    public static boolean tryBuildWithReservations(RobotType type) throws GameActionException {
        int reservedLead = Communication.getReservedLead();
        int reservedGold = Communication.getReservedGold();

        if (reservedLead != 0 || reservedGold != 0) {
            // There already exists a reservation
            int remainingLead = rc.getTeamLeadAmount(ALLY_TEAM) - type.buildCostLead;
            int remainingGold = rc.getTeamGoldAmount(ALLY_TEAM) - type.buildCostGold;
            if ((remainingLead < reservedLead && type.buildCostLead > 0)|| (remainingGold < reservedGold && type.buildCostGold > 0)) {
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
            justBuiltLocation = Cache.MY_LOCATION.add(bestDirection);
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
            justBuiltLocation = Cache.MY_LOCATION.add(bestDirection);
            return true;
        }
        return false;
    }
}
