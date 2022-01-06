package bot;

import battlecode.common.*;
import bot.util.*;

import static bot.util.Constants.*;

public class Builder implements RunnableBot {
    private static boolean suicidal;
    private static MapLocation spawnLoc;
    private static int spawnRound;

    @Override
    public void init() throws GameActionException {
        spawnLoc = rc.getLocation();
        suicidal = rc.getRoundNum() % 2 == 0;
        spawnRound = rc.getRoundNum();
    }

    @Override
    public void loop() throws GameActionException {
        tryRepair();
        if (suicidal) trySeppuku();
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (rc.isMovementReady()) {
            if (closestEnemyAttacker == null) {
                Util.tryExplore();
            } else {
                Util.tryKiteFrom(closestEnemyAttacker.location);
            }
        }
        tryRepair();
        if (rc.getTeamLeadAmount(ALLY_TEAM) >= 4000 && Math.random() < 0.5) {
            tryBuild(RobotType.LABORATORY);
        }
        tryBuild(RobotType.WATCHTOWER);
    }

    public static boolean tryRepair() throws GameActionException {
        if (!rc.isActionReady()) return false;
        RobotInfo closestRepairable = Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> r.health < r.type.health && rc.getType().canRepair(r.type));
        if (closestRepairable == null) return false;
        if (rc.canRepair(closestRepairable.location)) {
            rc.repair(closestRepairable.location);
            return true;
        } else {
            return Util.tryMove(closestRepairable.location);
        }
    }

    public static boolean tryBuild(RobotType type) throws GameActionException {
        if (!rc.isActionReady()) return false;
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

    public static boolean trySeppuku() throws GameActionException {
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
