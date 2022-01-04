package bot;

import battlecode.common.*;
import bot.util.Cache;
import bot.util.RunnableBot;
import bot.util.Util;

import static bot.util.Constants.ORDINAL_DIRECTIONS;
import static bot.util.Constants.rc;

public class Builder implements RunnableBot {
    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (rc.isMovementReady()) {
            if (closestEnemyAttacker == null) {
                Util.tryExplore();
            } else {
                Util.tryKiteFrom(closestEnemyAttacker.location);
            }
        }
        if (rc.isActionReady()) {
            RobotInfo closestAllyWatchtower = Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> r.type == RobotType.WATCHTOWER && r.health < r.type.health && rc.canRepair(r.location));
            if (closestAllyWatchtower != null) {
                rc.repair(closestAllyWatchtower.location);
            } else {
                tryBuild(RobotType.WATCHTOWER);
            }
        }
    }

    boolean tryBuild(RobotType type) throws GameActionException {
        if (!rc.isActionReady()) return false;
        for (Direction d: ORDINAL_DIRECTIONS) {
            MapLocation loc = rc.getLocation().add(d);
            if ((loc.x + loc.y) % 2 == 0 && loc.x % 2 == 0) {
                if (rc.canBuildRobot(type, d)) {
                    rc.buildRobot(type, d);
                    return true;
                }
            }
        }
        return false;
    }
}
