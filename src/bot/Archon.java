package bot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
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
            for (Direction d: Constants.getAttemptOrder(Util.randomAdjacentDirection())) {
                if (rc.canBuildRobot(type, d)) {
                    rc.buildRobot(type, d);
                    return true;
                }
            }
            Communication.reserve(type.buildCostGold, type.buildCostLead);
            return false;
        }
    }

    public static Direction getIdealBuildDirectionForMining() throws GameActionException {
        MapLocation[] leadLocations = rc.senseNearbyLocationsWithLead(ARCHON_VISION_DISTANCE_SQUARED);
        // TODO
        MapLocation bestLocation = null;
        if (bestLocation == null) {
            return null;
        } else {
            return Generated34.execute(bestLocation);
        }
    }
}
