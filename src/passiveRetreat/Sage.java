package passiveRetreat;

import battlecode.common.*;
import passiveRetreat.util.Cache;
import passiveRetreat.util.RunnableBot;
import passiveRetreat.util.Util;

import static passiveRetreat.util.Constants.rc;

public class Sage implements RunnableBot {
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.isActionReady()) {
            tryAttackMaxEcon();
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
                    Util.tryExplore();
                }
            }
        }
        if (rc.isActionReady()) {
            tryAttackMaxEcon();
        }
    }

    boolean tryAttackMaxEcon() throws GameActionException {
        RobotInfo bestRobot = null;
        double bestEcon = 0;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (rc.canAttack(robot.location)) {
                int damage = Math.min(robot.health, RobotType.SAGE.getDamage(1));
                double econScore = (double)damage / robot.type.health * (robot.type.buildCostLead + 5 * robot.type.buildCostGold);
                if (econScore > bestEcon) {
                    bestEcon = econScore;
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
