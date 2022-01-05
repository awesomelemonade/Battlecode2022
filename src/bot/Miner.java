package bot;

import battlecode.common.*;
import bot.util.*;

import java.util.function.Predicate;

import static bot.util.Cache.ALLY_ROBOTS;
import static bot.util.Constants.*;

public class Miner implements RunnableBot {
    int spawnRound;

    @Override
    public void init() throws GameActionException {
        spawnRound = rc.getRoundNum();
    }

    @Override
    public void loop() throws GameActionException {
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        tryMine(closestEnemyAttacker);
        if (rc.isMovementReady()) {
            // If first turn, just move away from our Archon (saves bytecode)
            if (rc.getRoundNum() == spawnRound) {
                for (Direction d : ORDINAL_DIRECTIONS) {
                    MapLocation loc = rc.getLocation().add(d);
                    if (rc.canSenseLocation(loc)) {
                        RobotInfo robot = rc.senseRobotAtLocation(loc);
                        if (robot != null && robot.team == ALLY_TEAM && robot.type == RobotType.ARCHON) {
                            Util.tryKiteFrom(robot.location);
                            break;
                        }
                    }
                }
            } else {
                if (closestEnemyAttacker != null) {
                    Util.tryKiteFrom(closestEnemyAttacker.location);
                } else {
                    int highestPb = 1;
                    MapLocation highestPbLoc = null;
                    int bef = Clock.getBytecodeNum();
                    MapLocation[] senseLocs = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), RobotType.MINER.visionRadiusSquared);
                    for (int i = senseLocs.length; --i >= 0;) {
                        MapLocation loc = senseLocs[i];
                        if (rc.onTheMap(loc)) {
                            int pb = rc.senseLead(loc);
                            if (pb > highestPb) {
                                highestPb = pb;
                                highestPbLoc = loc;
                            }
                        }
                    }
                    int aft = Clock.getBytecodeNum();
                    if (highestPbLoc != null) {
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
                int amount = rc.senseGold(loc);
                while (amount > 0) {
                    rc.mineGold(loc);
                    --amount;
                    if (!rc.isActionReady()) return;
                }
            }
        }
        for (Direction d : ALL_DIRECTIONS) {
            MapLocation loc = rc.getLocation().add(d);
            if (rc.onTheMap(loc)) {
                int amount = rc.senseLead(loc);
                while (amount > 1 || (amount == 1 && closestEnemyAttacker != null)) {
                    rc.mineLead(loc);
                    --amount;
                    if (!rc.isActionReady()) return;
                }
            }
        }
    }
}
