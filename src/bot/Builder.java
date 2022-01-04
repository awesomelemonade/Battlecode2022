package bot;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import bot.util.RunnableBot;
import bot.util.Util;

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
                Util.tryRandomMove();
            } else {
                Util.tryKiteFrom(closestEnemyAttacker.location);
            }
        }
    }
}
