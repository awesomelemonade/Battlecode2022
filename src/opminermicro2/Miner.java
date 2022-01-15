package opminermicro2;

import battlecode.common.*;
import opminermicro2.util.*;

import static opminermicro2.util.Cache.ALLY_ROBOTS;
import static opminermicro2.util.Constants.*;

public class Miner implements RunnableBot {
    private static int spawnRound;
    private static int amountMined;

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
                    Explorer.currentExploreDirection = null;
                    Util.tryKiteFrom(closestEnemyAttacker.location);
                } else {
                    debug_tryMoveGoodMining();
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
        if (amountMined > 0) {
            Communication.setMinedAmount(amountMined);
        }
    }

    private static double miningScore = -1;

    public static void debug_getMiningScore(MapLocation target) throws GameActionException {
        double cooldown = 1.0 + rc.senseRubble(target) / 10.0;
        double leadPerTurn = 5.0 / cooldown; // lead / turn
        int numLeadSquares = 0;
        double totalLead = 0;
        for (int i = ALL_DIRECTIONS.length; --i >= 0;) {
            MapLocation loc = target.add(ALL_DIRECTIONS[i]);
            if (rc.canSenseLocation(loc) && rc.onTheMap(loc)) {
                int amount = rc.senseLead(loc);
                if (amount > 0) {
                    totalLead += amount - 1;
                    numLeadSquares++;
                }
            }
        }
        int curDist = Cache.MY_LOCATION.distanceSquaredTo(target);
        double numMiners = LambdaUtil.arraysStreamSum(ALLY_ROBOTS, r -> r.type == RobotType.MINER && r.location.isWithinDistanceSquared(target, curDist)) + 1;
        double leadAmount = totalLead / numMiners; // lead

        // Maximize lead in k turns
        int k = 20; // turns
        int numRegenRounds = k / 40 + (((rc.getRoundNum() - 1) % 40 + (k % 40) >= 40) ? 1 : 0);
        leadAmount += numRegenRounds * numLeadSquares * 3.0 / numMiners;
        miningScore = Math.min(leadAmount, k * leadPerTurn);
    }

    private static boolean tryMoveGoodMiningRet;

    public static void debug_tryMoveGoodMining() throws GameActionException {
        if (!rc.isMovementReady()) {
            tryMoveGoodMiningRet = false;
            return;
        }

        MapLocation[] locs = rc.getAllLocationsWithinRadiusSquared(Cache.MY_LOCATION, RobotType.MINER.visionRadiusSquared);
        MapLocation bestLoc = null;
        double bestScore = 0;
        double cnt = 0;
        for (int i = locs.length; --i >= 0;) {
            MapLocation loc = locs[i];
            debug_getMiningScore(loc);
            if (miningScore > bestScore) {
                cnt = 1;
                bestScore = miningScore;
                bestLoc = loc;
            } else if (miningScore == bestScore) {
                cnt++;
                if (Math.random() < 1.0 / cnt) { // Maybe add random() to score instead of streaming random?
                    bestScore = miningScore;
                    bestLoc = loc;
                }
            }
        }
        Debug.setIndicatorString("score = " + bestScore);
        if (bestScore < 20) {
            tryMoveGoodMiningRet = false;
            return;
        }
        Debug.setIndicatorLine(Profile.MINING, Cache.MY_LOCATION, bestLoc, 0, 0, 0);
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
