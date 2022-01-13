package newbuildorder3;

import battlecode.common.*;
import newbuildorder3.util.Cache;
import newbuildorder3.util.Communication;
import newbuildorder3.util.RunnableBot;
import newbuildorder3.util.Util;

import static newbuildorder3.util.Constants.*;

public class Builder implements RunnableBot {
    private static int movesSinceAction;
    private static MapLocation spawnLoc;
    private static int spawnRound;
    private static MapLocation closestRepairableLocation;

    @Override
    public void init() throws GameActionException {
        movesSinceAction = 0;
        spawnLoc = rc.getLocation();
        spawnRound = rc.getRoundNum();
    }

    @Override
    public void loop() throws GameActionException {
        //canBuildLaboratory = LambdaUtil.arraysStreamSum(Cache.ALLY_ROBOTS, r -> r.type == RobotType.WATCHTOWER) >= 5;
        RobotInfo closestRepairable = Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> r.health < r.type.getMaxHealth(r.level) && rc.getType().canRepair(r.type));
        if (closestRepairable == null) {
            closestRepairableLocation = null;
        } else {
            closestRepairableLocation = closestRepairable.location;
        }
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
            tryAction();
            tryMove();
            tryAction();
        }
    }

    public static void tryAction() throws GameActionException {
        if (!rc.isActionReady()) return;
        if (tryRepair()) {
            movesSinceAction = 0;
            return;
        }
        int lead = rc.getTeamLeadAmount(ALLY_TEAM);
        if ((lead >= 5000 && Math.random() < 0.5) ||
                (lead >= 800 && rc.getRoundNum() > 1900)) {
            if (tryBuild(RobotType.LABORATORY)) {
                movesSinceAction = 0;
                return;
            }
        }
        if (tryBuild(RobotType.WATCHTOWER)) {
            movesSinceAction = 0;
            return;
        }
    }

    public static void tryMove() throws GameActionException {
        if (!rc.isMovementReady()) return;
        movesSinceAction++;
        if (movesSinceAction >= 40) {
            if (tryMoveToSeppuku()) {
                return;
            }
        }
        if (tryMoveToRepair()) {
            return;
        }
        // TODO: tryMoveToBuild()
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (closestEnemyAttacker == null) {
            Util.tryExplore();
        } else {
            Util.tryKiteFrom(closestEnemyAttacker.location);
        }
    }

    public static boolean tryRepair() throws GameActionException {
        if (closestRepairableLocation == null) return false;
        if (rc.canRepair(closestRepairableLocation)) {
            rc.repair(closestRepairableLocation);
            return true;
        }
        return false;
    }

    public static boolean tryMoveToRepair() throws GameActionException {
        if (closestRepairableLocation == null) return false;
        if (rc.getLocation().isWithinDistanceSquared(closestRepairableLocation, BUILDER_REPAIR_DISTANCE_SQUARED)) {
            Util.tryMove(closestRepairableLocation);
        }
        return true;
    }

    public static boolean tryBuild(RobotType type) throws GameActionException {
        for (Direction d: ORDINAL_DIRECTIONS) {
            MapLocation loc = rc.getLocation().add(d);
            if ((loc.x + loc.y) % 2 == 0 && loc.x % 2 == 0) {
                if (rc.canBuildRobot(type, d)) {
                    rc.buildRobot(type, d);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean tryMoveToSeppuku() throws GameActionException {
        // Checks for a square that has no lead and is in our territory. If one exists, go there and die.
        MapLocation[] candidateSquares = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);
        MapLocation bestLoc = null;
        int bestDistanceSquared = Integer.MAX_VALUE;
        for (int i = candidateSquares.length; --i >= 0;) {
            MapLocation loc = candidateSquares[i];
            if (!rc.onTheMap(loc)) continue;
            if (rc.senseLead(loc) == 0 && Communication.getChunkInfo(loc) == Communication.CHUNK_INFO_ALLY && (loc.equals(rc.getLocation()) || !rc.isLocationOccupied(loc))) {
                int distanceSquared = loc.distanceSquaredTo(spawnLoc);
                if (distanceSquared < bestDistanceSquared) {
                    bestLoc = loc;
                    bestDistanceSquared = distanceSquared;
                }
            }
        }
        if (bestLoc == null) return false;
        if (bestLoc.equals(rc.getLocation())) {
            rc.disintegrate();
            return true;
        } else {
            Util.tryMove(bestLoc);
            return true;
        }
    }
}
