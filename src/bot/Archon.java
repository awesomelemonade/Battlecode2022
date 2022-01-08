package bot;

import battlecode.common.*;
import bot.util.*;

import static bot.util.Constants.*;

public class Archon implements RunnableBot {
    private static final RobotType[] earlyGameBuildOrder = {RobotType.MINER, RobotType.MINER, RobotType.SOLDIER, RobotType.MINER, RobotType.SOLDIER};
    private static final RobotType[] lateGameBuildOrder = {RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.MINER, RobotType.BUILDER};
    private static int buildCount;
    private static boolean builtFirstLateGame;

    @Override
    public void init() throws GameActionException {
        buildCount = 0;
        builtFirstLateGame = false;
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getRoundNum() <= 100) {
            RobotType buildType = earlyGameBuildOrder[buildCount % earlyGameBuildOrder.length];
            if (buildType == RobotType.SOLDIER && rc.getTeamGoldAmount(ALLY_TEAM) >= RobotType.SAGE.buildCostGold) buildType = RobotType.SAGE;
            if (tryBuild(buildType)) {
                buildCount++;
            }
        } else {
            if (builtFirstLateGame == false) {
                buildCount = 0;
                builtFirstLateGame = true;
            }
            RobotType buildType = lateGameBuildOrder[buildCount % earlyGameBuildOrder.length];
            if (buildType == RobotType.SOLDIER && rc.getTeamGoldAmount(ALLY_TEAM) >= RobotType.SAGE.buildCostGold) buildType = RobotType.SAGE;
            else if (buildType == RobotType.BUILDER && rc.getTeamLeadAmount(ALLY_TEAM) < 200) buildType = RobotType.SOLDIER;
            if (tryBuild(buildType)) {
                buildCount++;
            }
        }
        tryRepair();
    }

    public static boolean tryBuild(RobotType type) throws GameActionException {
        int reservedLead = Communication.getReservedLead();
        int reservedGold = Communication.getReservedGold();

        if (reservedLead != 0 || reservedGold != 0) {
            // There already exists a reservation
            int remainingLead = rc.getTeamLeadAmount(ALLY_TEAM) - type.buildCostLead;
            int remainingGold = rc.getTeamGoldAmount(ALLY_TEAM) - type.buildCostGold;
            if (remainingLead < reservedLead || remainingGold < reservedGold) {
                return false;
            } else {
                for (Direction d: Constants.getAttemptOrder(Util.randomAdjacentDirection())) {
                    if (rc.canBuildRobot(type, d)) {
                        rc.buildRobot(type, d);
                        return true;
                    }
                }
                return false;
            }
        } else {
            // Build if we can, otherwise reserve
            Direction idealDirection = type == RobotType.MINER ? getIdealBuildDirectionForMining() : Util.randomAdjacentDirection();
            for (Direction d: Constants.getAttemptOrder(idealDirection)) {
                if (rc.canBuildRobot(type, d)) {
                    rc.buildRobot(type, d);
                    return true;
                }
            }
            Communication.reserve(type.buildCostGold, type.buildCostLead);
            return false;
        }
    }

    public static void tryRepair() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }
        if (rc.getTeamLeadAmount(ALLY_TEAM) < 500 && rc.senseRubble(Cache.MY_LOCATION) > 20) {
            return; // We should probably be saving to build units
        }
        MapLocation bestLocation = null;
        double bestScore = -Double.MAX_VALUE;
        if (Cache.ENEMY_ROBOTS.length > 0) {
            // If we see any enemies
            // Repair the lowest health so they can survive
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                RobotInfo robot = Cache.ALLY_ROBOTS[i];
                MapLocation location = robot.location;
                if (!rc.canRepair(location)) {
                    continue;
                }
                int health = robot.health;
                int maxHealth = robot.type.getMaxHealth(robot.level);
                if (health >= maxHealth) {
                    continue;
                }
                double score = health;
                if (score > bestScore) {
                    bestScore = score;
                    bestLocation = location;
                }
            }
        } else {
            // No enemies in sight
            // Repair the highest health that isn't full so they can leave (unclogging mechanism)
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                RobotInfo robot = Cache.ALLY_ROBOTS[i];
                MapLocation location = robot.location;
                if (!rc.canRepair(location)) {
                    continue;
                }
                int health = robot.health;
                int maxHealth = robot.type.getMaxHealth(robot.level);
                if (health >= maxHealth) {
                    continue;
                }
                double score = maxHealth - health;
                if (score > bestScore) {
                    bestScore = score;
                    bestLocation = location;
                }
            }
        }
        if (bestLocation != null) {
            rc.repair(bestLocation);
        }
    }

    public static Direction getIdealBuildDirectionForMining() throws GameActionException {
        MapLocation[] leadLocations = rc.senseNearbyLocationsWithLead(ARCHON_VISION_DISTANCE_SQUARED);
        MapLocation bestLocation = null;
        double bestScore = -Double.MAX_VALUE;
        for (int i = leadLocations.length; --i >= 0;) {
            MapLocation location = leadLocations[i];
            int lead = rc.senseLead(location);
            if (lead <= 6) {
                continue;
            }
            double score = lead + Math.random();
            if (score > bestScore) {
                bestScore = score;
                bestLocation = location;
            }
        }
        if (bestLocation == null) {
            return Util.randomAdjacentDirection();
        } else {
            int a = Clock.getBytecodeNum();
            Direction ret = Generated34.execute(bestLocation);
            int b = Clock.getBytecodeNum();
            Debug.setIndicatorLine(Profile.MINING, Cache.MY_LOCATION, bestLocation, 255, 255, 0);
            Debug.setIndicatorString("Gen34: " + (b - a) + " - " + ret);
            if (ret == null) {
                return Util.randomAdjacentDirection();
            } else {
                return ret;
            }
        }
    }
}
