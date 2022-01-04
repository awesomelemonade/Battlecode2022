package bot;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import bot.util.Cache;
import bot.util.Debug;
import bot.util.RunnableBot;

import static bot.util.Constants.rc;

public class Watchtower implements RunnableBot {
    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        if (rc.isActionReady()) {
            RobotInfo bestRobot = null;
            int bestHealth = (int)1e9;
            for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
                if (rc.canAttack(robot.location)) {
                    if (robot.health < bestHealth) {
                        bestHealth = robot.health;
                        bestRobot = robot;
                    }
                }
            }
            if (bestRobot != null) {
                rc.attack(bestRobot.location);
            }
        }
    }
}
