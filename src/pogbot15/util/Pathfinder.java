package pogbot15.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;

import static pogbot15.util.Constants.rc;

public class Pathfinder {
    public static int moveDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    public static boolean execute(MapLocation target) {
        Debug.setIndicatorLine(Profile.PATHFINDER, Cache.MY_LOCATION, target, 0, 0, 255);
        try {
            Direction d;
            switch (Constants.ROBOT_TYPE) {
                case MINER:
                case BUILDER:
                case LABORATORY:
                    d = Generated9.execute(target);
                    break;
                default:
                    d = Generated13.execute(target);
                    break;
            }
            if (d != null && d != Direction.CENTER) {
                return Util.tryMove(d);
            }
            return false;
        } catch (GameActionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean executeNaive(MapLocation target) {
        if (Cache.MY_LOCATION.equals(target)) {
            // already there
            return true;
        }
        // Out of all possible moves that lead to a lower euclidean distance OR lower move distance,
        // find the direction that goes to the highest passability
        // euclidean distance defined by dx^2 + dy^2
        // move distance defined by max(dx, dy)
        // ties broken by "preferred direction" dictated by Constants.getAttemptOrder
        int lowestRubble = Integer.MAX_VALUE;
        int targetDistanceSquared = Cache.MY_LOCATION.distanceSquaredTo(target) - 1; // subtract 1 to be strictly less
        int targetMoveDistance = moveDistance(Cache.MY_LOCATION, target);
        Direction bestDirection = null;
        for (Direction direction : Constants.getAttemptOrder(Cache.MY_LOCATION.directionTo(target))) {
            if (rc.canMove(direction)) {
                MapLocation location = Cache.MY_LOCATION.add(direction);
                if (location.isWithinDistanceSquared(target, targetDistanceSquared) || moveDistance(location, target) < targetMoveDistance) {
                    try {
                        int rubble = rc.senseRubble(location);
                        if (rubble < lowestRubble) {
                            lowestRubble = rubble;
                            bestDirection = direction;
                        }
                    } catch (GameActionException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
        }
        if (bestDirection != null) {
            Util.move(bestDirection);
            return true;
        }
        return false;
    }
}