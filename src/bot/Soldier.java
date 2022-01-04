package bot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import bot.util.Cache;
import bot.util.RunnableBot;
import bot.util.Util;

import static bot.util.Constants.rc;

public class Soldier implements RunnableBot {
    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        RobotInfo closestEnemy = Util.getClosestEnemyRobot();
        if (rc.isMovementReady()) {
            if (rc.isActionReady() && closestEnemy != null) {
                int best = (int)1e9;
                Direction bestDir = null;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        MapLocation loc = rc.getLocation().translate(dx, dy);
                        Direction dir = rc.getLocation().directionTo(loc);
                        if (dir == Direction.CENTER || rc.canMove(dir)) {
                            int dist = loc.distanceSquaredTo(closestEnemy.location);
                            if (Math.abs(dist - 13) < best) {
                                best = Math.abs(dist - 13);
                                bestDir = dir;
                            }
                        }
                    }
                }
                if (bestDir != null && bestDir != Direction.CENTER) {
                    Util.tryMove(bestDir);
                }
            } else {
                if (closestEnemyAttacker != null) {
                    Util.tryKiteFrom(closestEnemyAttacker.location);
                } else {
                    Util.tryRandomMove();
                }
            }
        }
        if (rc.isActionReady()) {
            tryAttackLowHealth();
        }
    }

    boolean tryAttackLowHealth() throws GameActionException {
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
            return true;
        }
        return false;
    }
}
