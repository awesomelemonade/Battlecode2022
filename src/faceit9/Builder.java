package faceit9;

import battlecode.common.*;
import faceit9.util.*;

import static faceit9.util.Constants.*;

public class Builder implements RunnableBot {
    private static RobotInfo[] ALLY_ROBOTS_ACTION_RADIUS = new RobotInfo[0];
    private static MapLocation justBuiltLocation = null;

    private static MapLocation cornerA;
    private static MapLocation cornerB;
    private static MapLocation cornerC;
    private static MapLocation cornerD;

    @Override
    public void init() throws GameActionException {
        cornerA = new MapLocation(-1, -1);
        cornerB = new MapLocation(MAP_WIDTH, -1);
        cornerC = new MapLocation(-1, MAP_HEIGHT);
        cornerD = new MapLocation(MAP_WIDTH, MAP_HEIGHT);
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
        if (Cache.ENEMY_ROBOTS.length == 0 && isStuckWithNeighboringLaboratoriesAtFullHealth()) {
            Communication.skipIncrementUnitCount = true;
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
        if (tryMoveTowardsLabLocation()) {
            return;
        }
        Util.tryExplore();
    }

    public static boolean tryMoveTowardsLabLocation() throws GameActionException {
        int distanceSquaredA = Cache.MY_LOCATION.distanceSquaredTo(cornerA);
        int distanceSquaredB = Cache.MY_LOCATION.distanceSquaredTo(cornerB);
        int distanceSquaredC = Cache.MY_LOCATION.distanceSquaredTo(cornerC);
        int distanceSquaredD = Cache.MY_LOCATION.distanceSquaredTo(cornerD);
        MapLocation bestCorner = null;
        int bestCornerDistanceSquared = Integer.MAX_VALUE;
        if (distanceSquaredA < bestCornerDistanceSquared) {
            bestCorner = cornerA;
            bestCornerDistanceSquared = distanceSquaredA;
        }
        if (distanceSquaredB < bestCornerDistanceSquared) {
            bestCorner = cornerB;
            bestCornerDistanceSquared = distanceSquaredB;
        }
        if (distanceSquaredC < bestCornerDistanceSquared) {
            bestCorner = cornerC;
            bestCornerDistanceSquared = distanceSquaredC;
        }
        if (distanceSquaredD < bestCornerDistanceSquared) {
            bestCorner = cornerD;
        }
        if (bestCorner == null) {
            return false;
        }
        if (bestCornerDistanceSquared > 100) { // Magic value
            Util.tryMove(bestCorner);
            return true;
        }

        // Go to best square nearest to corner with least rubble?
        double bestScore = Double.MAX_VALUE;
        MapLocation bestLocation = null;
        MapLocation[] vision = rc.getAllLocationsWithinRadiusSquared(Cache.MY_LOCATION, 5);
        for (int i = vision.length; --i >= 0;) {
            MapLocation location = vision[i];
            if (Cache.MY_LOCATION.equals(location) || rc.onTheMap(location) && !rc.isLocationOccupied(location)) {
                int distanceToCorner = bestCorner.distanceSquaredTo(location);
                if (distanceToCorner <= 5) {
                    distanceToCorner = 15 - distanceToCorner;
                }
                int rubble = rc.senseRubble(location);
                double score = rubble * 10000000.0 + distanceToCorner * 10000.0 + Cache.MY_LOCATION.distanceSquaredTo(location);
                if (score < bestScore) {
                    bestScore = score;
                    bestLocation = location;
                }
            }
        }
        if (bestLocation != null) {
            Debug.setIndicatorLine(Cache.MY_LOCATION, bestLocation, 128, 128, 128);
            Util.tryMove(bestLocation);
            return true;
        }
        return false;
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
        MapLocation enemyLocation = enemy == null ? null : enemy.location;
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
            switch (robot.type) {
                case LABORATORY:
                case ARCHON:
                case WATCHTOWER:
                    if (robot.health < robot.type.getMaxHealth(robot.level)) {
                        int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(robot.location);
                        if (distanceSquared < bestDistanceSquared) {
                            bestDistanceSquared = distanceSquared;
                            repairTarget = robot;
                        }
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
        for (Direction d: Constants.getAttemptOrder(directionToCorner())) {
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
        for (Direction d: Constants.getAttemptOrder(directionToCorner())) {
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

    public static Direction directionToCorner() {
        int distanceSquaredA = Cache.MY_LOCATION.distanceSquaredTo(cornerA);
        int distanceSquaredB = Cache.MY_LOCATION.distanceSquaredTo(cornerB);
        int distanceSquaredC = Cache.MY_LOCATION.distanceSquaredTo(cornerC);
        int distanceSquaredD = Cache.MY_LOCATION.distanceSquaredTo(cornerD);
        MapLocation bestLocation = null;
        int bestDistanceSquared = Integer.MAX_VALUE;
        if (distanceSquaredA < bestDistanceSquared) {
            bestLocation = cornerA;
            bestDistanceSquared = distanceSquaredA;
        }
        if (distanceSquaredB < bestDistanceSquared) {
            bestLocation = cornerB;
            bestDistanceSquared = distanceSquaredB;
        }
        if (distanceSquaredC < bestDistanceSquared) {
            bestLocation = cornerC;
            bestDistanceSquared = distanceSquaredC;
        }
        if (distanceSquaredD < bestDistanceSquared) {
            bestLocation = cornerD;
        }
        if (bestLocation == null) {
            return Util.randomAdjacentDirection();
        }
        Direction direction = Cache.MY_LOCATION.directionTo(bestLocation);
        return direction == Direction.CENTER ? Util.randomAdjacentDirection() : direction;
    }

    public static boolean isStuckWithNeighboringLaboratoriesAtFullHealth() throws GameActionException {
        for (int i = ORDINAL_DIRECTIONS.length; --i >= 0;) {
            MapLocation location = Cache.MY_LOCATION.add(ORDINAL_DIRECTIONS[i]);
            if (rc.onTheMap(location)) {
                // Check for laboratory
                RobotInfo robot = rc.senseRobotAtLocation(location);
                if (!(robot != null && robot.team == ALLY_TEAM && robot.type == RobotType.LABORATORY &&
                        robot.mode == RobotMode.TURRET && robot.health == robot.type.getMaxHealth(robot.level))) {
                    return false;
                }
            }
        }
        return true;
    }
}
