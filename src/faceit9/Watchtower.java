package faceit8;

import battlecode.common.*;
import faceit8.util.Cache;
import faceit8.util.Communication;
import faceit8.util.RunnableBot;
import faceit8.util.Util;

import static faceit8.util.Constants.rc;

public class Watchtower implements RunnableBot {
    private int turnsSinceUseful;
    private int turnsSinceAttack;

    @Override
    public void init() throws GameActionException {
        turnsSinceUseful = 0;
        turnsSinceAttack = 0;
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getMode() == RobotMode.TURRET) {
            if (Cache.ENEMY_ROBOTS.length == 0) {
                turnsSinceUseful++;
            } else {
                turnsSinceUseful = 0;
            }
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
                if (bestRobot == null) {
                    turnsSinceAttack++;
                } else {
                    rc.attack(bestRobot.location);
                    turnsSinceAttack = 0;
                }
            }
            if (turnsSinceUseful >= 10 || (turnsSinceAttack >= 10 && rc.getRoundNum() % 100 == 0)) {
                if (rc.canTransform()) {
                    rc.transform();
                }
            }
        } else if (rc.getMode() == RobotMode.PORTABLE) {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            if (closestEnemy == null) {
                MapLocation loc = Communication.getClosestEnemyChunk();
                if (loc == null) {
                    Util.tryExplore();
                } else {
                    Util.tryMove(loc);
                }
            } else {
                if (closestEnemy.location.isWithinDistanceSquared(rc.getLocation(), RobotType.WATCHTOWER.actionRadiusSquared)) {
                    if (rc.canTransform()) {
                        rc.transform();
                    }
                } else {
                    Util.tryMove(closestEnemy.location);
                }
            }
        }
    }
}
