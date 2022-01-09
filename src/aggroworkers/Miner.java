package aggroworkers;

import battlecode.common.*;
import aggroworkers.util.Debug;
import aggroworkers.util.Profile;
import aggroworkers.util.RunnableBot;
import aggroworkers.util.Util;
import aggroworkers.util.Cache;
import aggroworkers.util.Communication;
import aggroworkers.util.LambdaUtil;

import static aggroworkers.util.Cache.ALLY_ROBOTS;
import static aggroworkers.util.Constants.*;

public class Miner implements RunnableBot {
    int spawnRound;
    boolean isAggro;

    @Override
    public void init() throws GameActionException {
        spawnRound = rc.getRoundNum();
        if (spawnRound <= 3) {
            isAggro = true;
        }
    }

    @Override
    public void loop() throws GameActionException {
        if (isAggro) {
            performAggroStrat();
        } else {
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
                        if (!tryMoveGoodMining(false)) {
                            Util.tryExplore();
                        }
                    }
                }
                tryMine();
            }
        }
    }

    public void performAggroStrat() throws GameActionException {
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
            return;
        }
        boolean inEnemyTerritory = LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.type == RobotType.ARCHON || (r.type == RobotType.MINER && r.location.isWithinDistanceSquared(Cache.MY_LOCATION, 5)));
        if (SPAWN.isWithinDistanceSquared(Cache.MY_LOCATION, 34)) inEnemyTerritory = false;
        if (inEnemyTerritory) {
            tryMineDeplete();
            if (!tryMoveGoodMining(true)) {
                Util.tryExplore();
            }
            tryMineDeplete();
        } else {
            tryMine();
            Util.tryExplore();
            tryMine();
        }
    }

    public static boolean tryMoveGoodMining(boolean deplete) throws GameActionException {
        if (!rc.isMovementReady()) return false;

        MapLocation ourLoc = rc.getLocation();
        int ourX = ourLoc.x;
        int ourY = ourLoc.y;
        int totalLead = 0;
        double leadForceX = 0, leadForceY = 0;
        MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead(RobotType.MINER.visionRadiusSquared);
        if (leadLocs.length < 20) {
            for (int i = leadLocs.length; --i >= 0;) {
                MapLocation loc = leadLocs[i];
                if (loc.equals(rc.getLocation())) continue;
                double dx = loc.x - ourX;
                double dy = loc.y - ourY;
                double dis2 = loc.distanceSquaredTo(ourLoc);
                double dis = Math.sqrt(dis2);
                int leadAmount = rc.senseLead(loc);
                if (deplete) {
                    if (leadAmount < 10) {
                        leadAmount = 100 * (10 - leadAmount);
                    }
                } else {
                    leadAmount--;
                }
                totalLead += leadAmount;

                leadForceX += dx/dis * leadAmount/dis2;
                leadForceY += dy/dis * leadAmount/dis2;
            }
        } else {
            for (int i = leadLocs.length; --i >= 0;) {
                totalLead += rc.senseLead(leadLocs[i]);
            }
        }

        int totalMiners = 0;
        double minerForceX = 0, minerForceY = 0;
        for (int i = ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = ALLY_ROBOTS[i];
            if (robot.type == RobotType.MINER) {
                MapLocation loc = robot.location;
                double dx = loc.x - ourX;
                double dy = loc.y - ourY;
                double dis2 = loc.distanceSquaredTo(ourLoc);
                double dis = Math.sqrt(dis2);
                totalMiners++;

                minerForceX -= dx / dis * 1.0 / dis2;
                minerForceY -= dy / dis * 1.0 / dis2;
            }
        }

        // No good mining nearby, so we failed.
        if (totalLead - 10*totalMiners <= 0) return false;

        double forceX = leadForceX + 10 * minerForceX;
        double forceY = leadForceY + 10 * minerForceY;
        double forceMag = Math.hypot(forceX, forceY);

        // Force is nearly 0, so we're already at a good spot.
        if (forceMag <= 1e-8) return true;

        forceX = forceX / forceMag * 5;
        forceY = forceY / forceMag * 5;

        int targetX = (int) Math.round(ourX + forceX);
        int targetY = (int) Math.round(ourY + forceY);
        Debug.setIndicatorLine(Profile.MINING, ourLoc, new MapLocation(targetX, targetY), 0, 0, 0);

        Util.tryMove(new MapLocation(targetX, targetY));
        return true;
    }

    public static void tryMine() throws GameActionException {
        if (!rc.isActionReady()) return;
        boolean inEnemyTerritory = LambdaUtil.arraysAnyMatch(Cache.ENEMY_ROBOTS, r -> r.type == RobotType.ARCHON);
        if (inEnemyTerritory) {
            tryMineDeplete();
        }
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
