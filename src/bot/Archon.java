package bot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import bot.util.Constants;
import bot.util.RunnableBot;
import bot.util.Util;

import static bot.util.Constants.*;

public class Archon implements RunnableBot {
    RobotType[] earlyGameBuildOrder = {RobotType.MINER, RobotType.BUILDER, RobotType.MINER, RobotType.BUILDER};
    RobotType[] lateGameBuildOrder = {RobotType.SOLDIER, RobotType.SOLDIER, RobotType.BUILDER, RobotType.MINER, RobotType.BUILDER};
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
            if (tryBuild(buildType)) {
                buildCount++;
            }
        }
    }

    boolean tryBuild(RobotType type) throws GameActionException {
        if (!rc.isActionReady()) return false;
        for (Direction d: Constants.getAttemptOrder(Util.randomAdjacentDirection())) {
            if (rc.canBuildRobot(type, d)) {
                rc.buildRobot(type, d);
                return true;
            }
        }
        return false;
    }
}
