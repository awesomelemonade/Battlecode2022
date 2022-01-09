package newbuildorder;

import battlecode.common.*;
import newbuildorder.util.*;

import static newbuildorder.util.Cache.ALLY_ROBOTS;
import static newbuildorder.util.Constants.*;

public class Miner implements RunnableBot {
    int spawnRound;

    @Override
    public void init() throws GameActionException {
        spawnRound = rc.getRoundNum();
    }

    @Override
    public void loop() throws GameActionException {
        tryMine();
        if (rc.isMovementReady()) {
            // If first turn, just move away from our Archon (saves bytecode)
            if (rc.getRoundNum() == spawnRound) {
                for (Direction d : ORDINAL_DIRECTIONS) {
                    MapLocation loc = rc.getLocation().add(d);
                    if (rc.canSenseLocation(loc)) {
                        RobotInfo robot = rc.senseRobotAtLocation(loc);
                        if (robot != null && robot.team == ALLY_TEAM && robot.type == RobotType.ARCHON) {
                            tryKite(robot.location);
                            break;
                        }
                    }
                }
            } else {
                tryMove();
            }
            tryMine();
        }
    }

    public static void tryMove() throws GameActionException {
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (closestEnemyAttacker != null) {
            tryKite(closestEnemyAttacker.location);
            return;
        }
        MapLocation closestChunk = Communication.getClosestEnemyAttackerChunk();
        if (closestChunk != null) {
            if (closestChunk.isWithinDistanceSquared(rc.getLocation(), 40)) {
                tryKite(closestChunk);
                return;
            }
        }
        if (!tryMoveGoodMining()) {
            Util.tryExplore();
        }
    }

    public static boolean tryMoveGoodMining() throws GameActionException {
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

    public static void tryKite(MapLocation location) throws GameActionException {
        Debug.setIndicatorLine(Profile.MINING, Cache.MY_LOCATION, location, 255, 0, 255);
        double bestScore = 0;
        double curDist = rc.getLocation().distanceSquaredTo(location);
        Direction bestDir = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation loc = rc.getLocation().translate(dx, dy);
                Direction dir = rc.getLocation().directionTo(loc);
                if (dir == Direction.CENTER || rc.canMove(dir)) {
                    int dist = loc.distanceSquaredTo(location);
                    if (dist < curDist) continue;
                    double distScore = dist - curDist;
                    double cooldown = 1.0 + rc.senseRubble(loc)/10.0;
                    double cdScore = 1.0 / cooldown;
                    double score = distScore + 10*cdScore;
                    if (score > bestScore) {
                        bestScore = score;
                        bestDir = dir;
                    }
                }
            }
        }
        if (bestDir != null && bestDir != Direction.CENTER) {
            Util.tryMove(bestDir);
        }
    }
}
