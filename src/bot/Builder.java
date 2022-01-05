package bot;

import battlecode.common.*;
import bot.util.Cache;
import bot.util.Communication;
import bot.util.RunnableBot;
import bot.util.Util;

import static bot.util.Constants.*;

public class Builder implements RunnableBot {
    boolean suicidal;
    MapLocation spawnLoc;
    int spawnRound;

    @Override
    public void init() throws GameActionException {
        spawnLoc = rc.getLocation();
        suicidal = rc.getRoundNum() % 2 == 0;
        spawnRound = rc.getRoundNum();
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getRoundNum() == spawnRound) {
            for (Direction d : ORDINAL_DIRECTIONS) {
                MapLocation loc = rc.getLocation().add(d);
                if (rc.canSenseLocation(loc)) {
                    RobotInfo robot = rc.senseRobotAtLocation(loc);
                    if (robot != null && robot.team == ALLY_TEAM && robot.type == RobotType.ARCHON) {
                        Util.tryKiteFrom(robot.location);
                        return;
                    }
                }
            }
        }
        if (rc.isActionReady()) {
                RobotInfo closestAllyWatchtower = Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> r.type == RobotType.WATCHTOWER && r.health < r.type.health && rc.canRepair(r.location));
            if (closestAllyWatchtower != null) {
                rc.repair(closestAllyWatchtower.location);
            } else {
                tryBuild(RobotType.WATCHTOWER);
            }
        }
        if (suicidal) trySeppuku();
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (rc.isMovementReady()) {
            if (closestEnemyAttacker == null) {
                Util.tryExplore();
            } else {
                Util.tryKiteFrom(closestEnemyAttacker.location);
            }
        }
        if (rc.isActionReady()) {
            RobotInfo closestAllyWatchtower = Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> r.type == RobotType.WATCHTOWER && r.health < r.type.health && rc.canRepair(r.location));
            if (closestAllyWatchtower != null) {
                rc.repair(closestAllyWatchtower.location);
            } else {
                tryBuild(RobotType.WATCHTOWER);
            }
        }
    }

    boolean tryBuild(RobotType type) throws GameActionException {
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

    boolean trySeppuku() throws GameActionException {
        // Checks for a square that has no lead and is in our territory. If one exists, go there and die.
        MapLocation[] candidateSquares = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 2);
        MapLocation bestLoc = null;
        for (int i = candidateSquares.length; --i >= 0;) {
            MapLocation loc = candidateSquares[i];
            if (!rc.onTheMap(loc)) continue;
            if (rc.senseLead(loc) == 0 && Communication.getChunkInfo(loc) == Communication.CHUNK_INFO_ALLY && (loc.equals(rc.getLocation()) || !rc.isLocationOccupied(loc))) {
                if (bestLoc == null || loc.distanceSquaredTo(spawnLoc) < bestLoc.distanceSquaredTo(spawnLoc)) {
                    bestLoc = loc;
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
