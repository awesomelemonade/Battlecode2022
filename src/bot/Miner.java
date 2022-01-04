package bot;

import battlecode.common.*;
import bot.util.*;

import java.util.function.Predicate;

import static bot.util.Cache.ALLY_ROBOTS;
import static bot.util.Constants.*;

public class Miner implements RunnableBot {
    @Override
    public void init() throws GameActionException {
    }

    @Override
    public void loop() throws GameActionException {
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        tryMine(closestEnemyAttacker);
        if (rc.isMovementReady()) {
            if (closestEnemyAttacker != null) {
                Util.tryKiteFrom(closestEnemyAttacker.location);
            } else {
                int highestAu = 0;
                MapLocation highestAuLoc = null;
                int highestPb = 0;
                MapLocation highestPbLoc = null;
                MapLocation[] senseLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.MINER.visionRadiusSquared);
                for (MapLocation loc : senseLocs) {
                    if (rc.onTheMap(loc)) {
                        int au = rc.senseGold(loc);
                        int pb = rc.senseLead(loc);

                        if (au > 0) {
                            if (highestAuLoc == null || au > highestAu) {
                                highestAu = au;
                                highestAuLoc = loc;
                            }
                        }
                        if (pb > 1) {
                            if (highestPbLoc == null || pb > highestPb) {
                                highestPb = pb;
                                highestPbLoc = loc;
                            }
                        }
                    }
                }
                if (highestAuLoc != null) {
                    Util.tryMove(highestAuLoc);
                } else if (highestPbLoc != null) {
                    Util.tryMove(highestPbLoc);
                } else {
                    RobotInfo closestAllyMiner = Util.getClosestRobot(ALLY_ROBOTS, r -> r.type == RobotType.MINER);
                    if (closestAllyMiner != null) {
                        Util.tryKiteFrom(closestAllyMiner.location);
                    } else {
                        Util.tryExplore();
                    }
                }
            }
            tryMine(closestEnemyAttacker);
        }
    }

    void tryMine(RobotInfo closestEnemyAttacker) throws GameActionException {
        // Try to mine as much Au as possible, then as much Pb as possible. Deplete Pb to -1 only if there is an enemy soldier/sage in vision.
        if (!rc.isActionReady()) return;
        for (Direction d : ALL_DIRECTIONS) {
            MapLocation loc = rc.getLocation().add(d);
            if (rc.onTheMap(loc)) {
                if (rc.canMineGold(loc)) {
                    rc.mineGold(loc);
                    if (!rc.isActionReady()) return;
                }
            }
        }
        for (Direction d : ALL_DIRECTIONS) {
            MapLocation loc = rc.getLocation().add(d);
            if (rc.onTheMap(loc)) {
                int amount = rc.senseLead(loc);
                if (amount > 1 || closestEnemyAttacker != null) {
                    if (rc.canMineLead(loc)) {
                        rc.mineLead(loc);
                        if (!rc.isActionReady()) return;
                    }
                }
            }
        }
    }
}
