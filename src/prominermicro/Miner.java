package prominermicro;

import battlecode.common.*;
import prominermicro.util.*;

import static prominermicro.util.Cache.ALLY_ROBOTS;
import static prominermicro.util.Constants.*;

public class Miner implements RunnableBot {
    int spawnRound;
    static int amountMined;

    @Override
    public void init() throws GameActionException {
        spawnRound = rc.getRoundNum();
    }

    @Override
    public void loop() throws GameActionException {
        amountMined = 0;
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        boolean inEnemyTerritory = LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.type == RobotType.ARCHON) && !LambdaUtil.arraysAnyMatch(ALLY_ROBOTS, r -> r.type == RobotType.ARCHON);
        if (inEnemyTerritory) {
            tryMineDeplete();
        } else {
            tryMine();
        }
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
                    Explorer.currentExploreDirection = -1.0;
                } else {
                    debug_tryMoveGoodMining();
                    if (!retvalue) {
                        Util.tryExplore();
                    }
                }
            }
            if (inEnemyTerritory) {
                tryMineDeplete();
            } else {
                tryMine();
            }
        }
        if (amountMined > 0) {
            Communication.setMinedAmount(amountMined);
        }
    }

    static boolean retvalue;
    public static void debug_tryMoveGoodMining() throws GameActionException {
        if (!rc.isMovementReady()) {
            retvalue = false;
            return;
        }

        // Scan through all locations in vision that are not occupied and adjacent to >1 lead piles
        // Score based on rubble and distance
        double bestScore = -Double.MAX_VALUE;
        MapLocation bestLoc = null;
        MapLocation[] inVision = rc.getAllLocationsWithinRadiusSquared(Cache.MY_LOCATION, RobotType.MINER.visionRadiusSquared);
        for (MapLocation loc : inVision) {
            if (!loc.equals(Cache.MY_LOCATION) && (!rc.canSenseLocation(loc) || rc.isLocationOccupied(loc))) continue;
            boolean hasResources = false;
            for (Direction d : ALL_DIRECTIONS) {
                MapLocation adjLoc = loc.add(d);
                if (!rc.canSenseLocation(adjLoc)) continue;
                if (rc.senseLead(adjLoc) > 1 || rc.senseGold(adjLoc) > 0) {
                    hasResources = true;
                    break;
                }
            }
            if (!hasResources) continue;
            double distScore = 1.0 - Math.sqrt(Cache.MY_LOCATION.distanceSquaredTo(loc)) / Math.sqrt(RobotType.MINER.visionRadiusSquared);
            double cooldown = 1.0 + rc.senseRubble(loc) / 10.0;
            double cdScore = 1.0 / cooldown;
            double score = distScore + 10.0 * cdScore;
            if (score > bestScore) {
                bestScore = score;
                bestLoc = loc;
            }
        }
        if (bestLoc == null) {
            retvalue = false;
            return;
        }
        rc.setIndicatorLine(Cache.MY_LOCATION, bestLoc, 0, 0, 0);
        Util.tryMove(bestLoc);
        retvalue = true;
        return;
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
                        amountMined++;
                        if (!rc.isActionReady()) return;
                    case 4:
                        rc.mineLead(loc);
                        amountMined++;
                        if (!rc.isActionReady()) return;
                    case 3:
                        rc.mineLead(loc);
                        amountMined++;
                        if (!rc.isActionReady()) return;
                    case 2:
                        rc.mineLead(loc);
                        amountMined++;
                        if (!rc.isActionReady()) return;
                    case 1:
                    case 0:
                }
            }
        }
    }

    public static void tryMineDeplete() throws GameActionException {
        if (!rc.isActionReady()) return;
        MapLocation current = rc.getLocation();
        while (true) {
            MapLocation bestLoc = null;
            int bestAmount = 150;
            for (int i = ALL_DIRECTIONS.length; --i >= 0;) {
                MapLocation loc = current.add(ALL_DIRECTIONS[i]);
                if (rc.onTheMap(loc)) {
                    int amount = rc.senseLead(loc);
                    if (amount > 0 && amount < bestAmount) {
                        bestAmount = amount;
                        bestLoc = loc;
                    }
                }
            }
            if (bestLoc == null) break;
            switch (bestAmount) {
                default:
                    amountMined++;
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 4:
                    amountMined++;
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 3:
                    amountMined++;
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 2:
                    amountMined++;
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 1:
                    amountMined++;
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 0:
            }
        }
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
    }
}
