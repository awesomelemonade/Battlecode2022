package bot;

import battlecode.common.*;
import bot.util.*;

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
        tryMine();
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
                    int highestResource = 0;
                    MapLocation highestResourceLocation = null;
                    MapLocation[] miningLocations = rc.senseNearbyLocationsWithGold(MINER_VISION);
                    if (miningLocations.length == 0) {
                        miningLocations = rc.senseNearbyLocationsWithLead(MINER_VISION);
                    }
                    for (int i = miningLocations.length; --i >= 0;) {
                        MapLocation loc = miningLocations[i];
                        int pb = rc.senseLead(loc);
                        if (pb > highestResource) {
                            highestResource = pb;
                            highestResourceLocation = loc;
                        }
                    }
                    if (highestResourceLocation != null) {
                        Util.tryMove(highestResourceLocation);
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
            tryMine();
        }
    }

    public static void tryMine() throws GameActionException {
        // Try to mine as much Au as possible, then as much Pb as possible. Deplete Pb to -1 only if there is an enemy soldier/sage in vision.
        if (!rc.isActionReady()) return;
        MapLocation current = rc.getLocation();
        for (int i = ALL_DIRECTIONS.length; --i >= 0;) {
            MapLocation loc = current.add(ALL_DIRECTIONS[i]);
            if (rc.onTheMap(loc)) {
                int amount = rc.senseGold(loc);
                switch (amount) {
                    default:
                        rc.mineGold(loc);
                        if (!rc.isActionReady()) return;
                    case 4:
                        rc.mineGold(loc);
                        if (!rc.isActionReady()) return;
                    case 3:
                        rc.mineGold(loc);
                        if (!rc.isActionReady()) return;
                    case 2:
                        rc.mineGold(loc);
                        if (!rc.isActionReady()) return;
                    case 1:
                        rc.mineGold(loc);
                        if (!rc.isActionReady()) return;
                    case 0:
                }
            }
        }
        for (int i = ALL_DIRECTIONS.length; --i >= 0;) {
            MapLocation loc = current.add(ALL_DIRECTIONS[i]);
            if (rc.onTheMap(loc)) {
                int amount = rc.senseLead(loc);
                switch (amount) {
                    default:
                        rc.mineLead(loc);
                        if (!rc.isActionReady()) return;
                    case 4:
                        rc.mineLead(loc);
                        if (!rc.isActionReady()) return;
                    case 3:
                        rc.mineLead(loc);
                        if (!rc.isActionReady()) return;
                    case 2:
                        rc.mineLead(loc);
                        if (!rc.isActionReady()) return;
                    case 1:
                    case 0:
                }
            }
        }
    }
}
