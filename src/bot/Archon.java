package bot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import bot.util.Communication;
import bot.util.Constants;
import bot.util.RunnableBot;
import bot.util.Util;

import static bot.util.Constants.*;

public class Archon implements RunnableBot {
    RobotType[] earlyGameBuildOrder = {RobotType.MINER, RobotType.MINER, RobotType.SOLDIER, RobotType.MINER, RobotType.SOLDIER};
    RobotType[] lateGameBuildOrder = {RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.MINER, RobotType.BUILDER};
    int buildCount;
    boolean builtFirstLateGame;

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

    boolean tryBuild(RobotType type) throws GameActionException {
        int reservedLead = Communication.getReservedLead();
        int reservedGold = Communication.getReservedGold();

        if (reservedLead != 0 || reservedGold != 0) {
            // There already exists a reservation
            int remainingLead = rc.getTeamLeadAmount(ALLY_TEAM) - type.buildCostLead;
            int remainingGold = rc.getTeamGoldAmount(ALLY_TEAM) - type.buildCostGold;
            if (remainingLead < reservedLead || remainingGold < reservedGold) return false;
            else {
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
}
