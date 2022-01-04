package bot;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import bot.util.Constants;
import bot.util.Debug;
import bot.util.RunnableBot;
import bot.util.Util;

import java.util.function.Predicate;

import static bot.util.Constants.rc;

public class Miner implements RunnableBot {
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
        // Try to mine as much Au as possible, then as much Pb as possible. Deplete Pb to 0 only if there is an enemy soldier/sage in vision.
        miningloop:
        while (rc.isActionReady()) {
            // If gold exists, mine it.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    MapLocation loc = rc.getLocation().translate(dx, dy);
                    if (rc.canMineGold(loc)) {
                        rc.mineGold(loc);
                        continue miningloop;
                    }
                }
            }

            // Try to mine Pb, all the way to 0 only if enemy attacker seen.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    MapLocation loc = rc.getLocation().translate(dx, dy);
                    if (rc.onTheMap(loc)) {
                        int amount = rc.senseLead(loc);
                        if (amount >= 2 || closestEnemyAttacker != null) {
                            if (rc.canMineLead(loc)) {
                                rc.mineLead(loc);
                                continue miningloop;
                            }
                        }
                    }
                }
            }
            break;
        }
    }
}
