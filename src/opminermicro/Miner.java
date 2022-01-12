package opminermicro;

import battlecode.common.*;
import opminermicro.util.*;

import static opminermicro.util.Cache.ALLY_ROBOTS;
import static opminermicro.util.Constants.*;

public class Miner implements RunnableBot {
    int spawnRound;

    @Override
    public void init() throws GameActionException {
        spawnRound = rc.getRoundNum();
    }

    @Override
    public void loop() throws GameActionException {
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
                } else {
                    debug_tryMoveGoodMining();;
                    if (!tryMoveGoodMiningRet) {
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
    }

    private static double score = -1;

    public static void debug_getMiningScore(MapLocation target) throws GameActionException {
        double cooldown = 1.0 + rc.senseRubble(target) / 10.0;
        double leadPerTurn = 5.0 / cooldown; // lead / turn
        int numLeadSquares = 0;
        double totalLead = 0;
        for (Direction d: ALL_DIRECTIONS) {
            MapLocation loc = target.add(d);
            if (rc.canSenseLocation(loc) && rc.onTheMap(loc)) {
                int amount = rc.senseLead(loc);
                if (amount > 0) {
                    totalLead += amount - 1;
                    numLeadSquares++;
                }
            }
        }
        int curDist = Cache.MY_LOCATION.distanceSquaredTo(target);
        double numMiners = LambdaUtil.arraysStreamSum(ALLY_ROBOTS, r -> r.type == RobotType.MINER && r.location.isWithinDistanceSquared(target, curDist - 1)) + 1;
        double leadAmount = totalLead / numMiners; // lead

        // Maximize lead in k turns
        int k = 30; // turns
        int numRegenRounds = k / 20 + (((rc.getRoundNum()-1) % 20 + (k % 20) >= 20) ? 1 : 0);
        leadAmount += numRegenRounds * numLeadSquares * 5.0 / numMiners;
        score = Math.min(leadAmount, k * leadPerTurn);
    }

    static boolean tryMoveGoodMiningRet;

    public static void debug_tryMoveGoodMining() throws GameActionException {
        if (!rc.isMovementReady()) {
            tryMoveGoodMiningRet = false;
            return;
        }

        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(Cache.MY_LOCATION, RobotType.MINER.visionRadiusSquared);
        MapLocation bestLoc = null;
        double bestScore = 0;
        double cnt = 0;
        for (MapLocation loc : locs) {
            debug_getMiningScore(loc);
            if (score > bestScore) {
                cnt = 1;
                bestScore = score;
                bestLoc = loc;
            } else if (score == bestScore) {
                cnt++;
                if (Math.random() < 1.0 / cnt) {
                    bestScore = score;
                    bestLoc = loc;
                }
            }
        }
        Debug.setIndicatorString("score = " + bestScore);
        if (bestScore < 30) {
            tryMoveGoodMiningRet = false;
            return;
        }
        Debug.setIndicatorLine(Cache.MY_LOCATION, bestLoc, 0, 0, 0);
        Util.tryMove(bestLoc);
        tryMoveGoodMiningRet = true;
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
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 4:
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 3:
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 2:
                    rc.mineLead(bestLoc);
                    if (!rc.isActionReady()) return;
                case 1:
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
