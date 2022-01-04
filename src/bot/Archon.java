package bot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotType;
import bot.util.RunnableBot;

import static bot.util.Constants.ORDINAL_DIRECTIONS;
import static bot.util.Constants.rc;

public class Archon implements RunnableBot {
    RobotType[] earlyGameBuildOrder = {RobotType.MINER, RobotType.MINER, RobotType.MINER, RobotType.BUILDER};
    RobotType[] lateGameBuildOrder = {RobotType.SOLDIER, RobotType.SOLDIER, RobotType.MINER, RobotType.SOLDIER, RobotType.BUILDER};
    int buildCount;
    boolean builtFirstLateGame;

    @Override
    public void init() throws GameActionException {
        buildCount = 0;
        builtFirstLateGame = false;
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getRoundNum() <= 30) {
            if (tryBuild(earlyGameBuildOrder[buildCount % earlyGameBuildOrder.length])) {
                buildCount++;
            }
        } else {
            if (builtFirstLateGame == false) {
                buildCount = 0;
                builtFirstLateGame = true;
            }
            if (tryBuild(lateGameBuildOrder[buildCount % lateGameBuildOrder.length])) {
                buildCount++;
            }
        }
    }

    boolean tryBuild(RobotType type) throws GameActionException {
        if (!rc.isActionReady()) return false;
        for (Direction d: ORDINAL_DIRECTIONS) {
            if (rc.canBuildRobot(type, d)) {
                rc.buildRobot(type, d);
                return true;
            }
        }
        return false;
    }
}
