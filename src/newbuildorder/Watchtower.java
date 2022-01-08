package newbuildorder;

import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import newbuildorder.util.Cache;
import newbuildorder.util.RunnableBot;

import static newbuildorder.util.Constants.rc;

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
