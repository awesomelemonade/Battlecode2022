package bot.util;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import static bot.util.Constants.rc;

public class Pathfinder {
    private static boolean bugpathBlocked = false;
    private static Direction bugpathDir;
    private static int bugpathTermCount = 0;
    private static Direction moveableTwo;
    private static double[] bugpathCache = new double[8];
    private static MapLocation prevLoc = Cache.MY_LOCATION;
    private static int bugpathTurnCount = 0;
    private static MapLocation prevTarget;

    public static int moveDistance(MapLocation a, MapLocation b) {
        return Math.max(Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }

    // returns cos(angle)
    public static double getCosAngle(MapLocation a, MapLocation b, MapLocation c) {
        // vector c->a and c->b
        int dx1 = a.x - c.x;
        int dy1 = a.y - c.y;
        int dx2 = b.x - c.x;
        int dy2 = b.y - c.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2));
    }

    public static Direction getTwoStepMoveHeuristic(MapLocation target) throws GameActionException {
        Direction next = null;
        double heuristicValue = -1024;
        double heuristicValueMoveable = -1024;
        moveableTwo = null;

        for (int i = Constants.ORDINAL_DIRECTIONS.length; --i >= 0; ) {
            Direction direction = Constants.ORDINAL_DIRECTIONS[i];
            MapLocation nextLoc = Cache.MY_LOCATION.add(direction);
            if (nextLoc.equals(prevLoc)) {
                continue;
            }
            double tempHeuristic = -1024;
            switch (direction) {
                case NORTH:
                    tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc));
                    break;
                case NORTHEAST:
                    tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
                    break;
                case EAST:
                    tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
                    break;
                case SOUTHEAST:
                    tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.EAST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
                    break;
                case SOUTH:
                    tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc));
                    break;
                case SOUTHWEST:
                    tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.SOUTH), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHEAST), target, nextLoc));
                    break;
                case WEST:
                    tempHeuristic = Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc));
                    break;
                case NORTHWEST:
                    tempHeuristic = Math.max(Math.max(Math.max(Math.max(getMoveHeuristic(nextLoc.add(Direction.NORTHEAST), target, nextLoc), getMoveHeuristic(nextLoc.add(Direction.SOUTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.WEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTHWEST), target, nextLoc)), getMoveHeuristic(nextLoc.add(Direction.NORTH), target, nextLoc));
                    break;
            }
            bugpathCache[i] = getMoveHeuristic(nextLoc, target, Cache.MY_LOCATION);
            tempHeuristic += bugpathCache[i];
            if (tempHeuristic > heuristicValue) {
                next = direction;
                heuristicValue = tempHeuristic;
            }
            if (rc.canMove(direction) && tempHeuristic > heuristicValueMoveable) {
                moveableTwo = direction;
                heuristicValueMoveable = tempHeuristic;
            } else {
                bugpathCache[i] = -1024;
            }
        }
        return next;
    }

    public static double getMoveHeuristic(MapLocation nextLoc, MapLocation target, MapLocation curLoc) throws GameActionException {
        if (!rc.onTheMap(nextLoc)) {
            return -1024;
        }
        // double angle = getAngle(nextLoc, target, curLoc);
        double passability = -2.0 * Math.log(1.0 + rc.senseRubble(nextLoc) / 10.0);
        double closer = Math.sqrt(curLoc.distanceSquaredTo(target)) - Math.sqrt(nextLoc.distanceSquaredTo(target));
        return (passability + closer * 0.8);
    }

    public static Direction getBestBugpathHeuristic(MapLocation bugpathLoc) throws GameActionException {
        // termination condition will be a heuristic of passability, angle to target, and angle to bugpath dir
        Direction next = null;
        double heuristicValue = -1024;
        for (int i = Constants.ORDINAL_DIRECTIONS.length; --i >= 0; ) {
            Direction direction = Constants.ORDINAL_DIRECTIONS[i];
            MapLocation nextLoc = Cache.MY_LOCATION.add(direction);
            double tempHeuristic = bugpathCache[i];
            if (tempHeuristic == -1024) {
                continue;
            }
            // tempHeuristic += getAngle(nextLoc, bugpathLoc, Cache.MY_LOCATION);
            tempHeuristic += Math.sqrt(Cache.MY_LOCATION.distanceSquaredTo(bugpathLoc)) - Math.sqrt(nextLoc.distanceSquaredTo(bugpathLoc)) * 0.8;
            // tempHeuristic += (moveDistance(Cache.MY_LOCATION, bugpathLoc) - moveDistance(nextLoc, bugpathLoc)) * 2 + 3;
            if (tempHeuristic > heuristicValue) {
                next = direction;
                heuristicValue = tempHeuristic;
            }
        }
        return next;
    }

    public static boolean execute(MapLocation target) {
        Debug.setIndicatorLine(Profile.PATHFINDER, Cache.MY_LOCATION, target, 0, 0, 255);
        boolean res;
        if (Cache.MY_LOCATION.distanceSquaredTo(target) <= 2) {
            if (rc.canMove(Cache.MY_LOCATION.directionTo(target))) {
                Util.move(Cache.MY_LOCATION.directionTo(target));
                res = true;
            } else {
                res = executeNaive(target);
            }
        } else {
            res = executeBugpath(target);
        }
        prevLoc = Cache.MY_LOCATION;
        prevTarget = target;
        return res;
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

    public static Direction greedyMove(MapLocation target) {
        int leastDist = target.distanceSquaredTo(Cache.MY_LOCATION);
        Direction best = null;
        for (int i = Constants.ORDINAL_DIRECTIONS.length; --i >= 0; ) {
            Direction direction = Constants.ORDINAL_DIRECTIONS[i];
            MapLocation nextLoc = Cache.MY_LOCATION.add(direction);
            int temp_dist = nextLoc.distanceSquaredTo(target);
            if (temp_dist < leastDist && rc.canMove(direction)) {
                leastDist = temp_dist;
                best = direction;
            }
        }
        return best;
    }

    public static boolean executeFallback(MapLocation target, Direction next) {
        Direction greedy = greedyMove(target);
        if (greedy == next) {
            bugpathBlocked = false;
        }
        if (greedy != null) {
            Util.move(greedy);
            return true;
        }
        return false;
    }

    public static boolean executeBugpath(MapLocation target) {
        if (Cache.MY_LOCATION.equals(target)) {
            // already there
            return true;
        }

        if (!target.equals(prevTarget)) {
            bugpathTurnCount = 0;
        }

        Direction twoStepMoveHeuristic = null;
        try {
            twoStepMoveHeuristic = getTwoStepMoveHeuristic(target);
        } catch (GameActionException ex) {
            throw new IllegalStateException(ex);
        }

        if (bugpathBlocked && bugpathTurnCount > 4) {
            // Util.println("BUGPATHFAIL: " + Cache.MY_LOCATION);
            return executeFallback(target, twoStepMoveHeuristic);
        }
        if (!bugpathBlocked && Cache.MY_LOCATION.add(twoStepMoveHeuristic).distanceSquaredTo(target) < Cache.MY_LOCATION.distanceSquaredTo(target)) {
            if (rc.canMove(twoStepMoveHeuristic)) {
                Util.move(twoStepMoveHeuristic);
                return true;
            } else {
                // try going to moveable two step
                // maybe lower bound heuristic threshold here
                if (moveableTwo != null && rc.canMove(moveableTwo)) {
                    Util.move(moveableTwo);
                    return true;
                }
            }
        } else {
            if (Cache.MY_LOCATION.distanceSquaredTo(target) <= 8) {
                bugpathBlocked = false;
                bugpathTurnCount = 0;
                return executeNaive(target);
            }
            if (!bugpathBlocked) {
                bugpathBlocked = true;
                bugpathDir = twoStepMoveHeuristic;
                bugpathTermCount = 0;
                bugpathTurnCount = 0;
            }
            MapLocation bugpathTarget = Cache.MY_LOCATION.add(bugpathDir).add(bugpathDir);
            Direction bestBugPathHeuristicDirection;
            try {
                bestBugPathHeuristicDirection = getBestBugpathHeuristic(bugpathTarget);
            } catch (GameActionException ex) {
                throw new IllegalStateException(ex);
            }

            // if next move moves backwards then we cancel bugpathing
            if (bestBugPathHeuristicDirection != null) {
                MapLocation nextLoc = Cache.MY_LOCATION.add(bestBugPathHeuristicDirection);
                double cosAngle = getCosAngle(nextLoc, target, Cache.MY_LOCATION);
                if (cosAngle < -0.5) {
                    bugpathTurnCount = 5;
                    return executeFallback(target, bestBugPathHeuristicDirection);
                }
                if (rc.canMove(bestBugPathHeuristicDirection)) {
                    Util.move(bestBugPathHeuristicDirection);
                    bugpathTurnCount++;
                    // terminating condition is if next square we move to is closer to target AND angle between direction we moved and target is greater than some threshold
                    if (nextLoc.distanceSquaredTo(target) < Cache.MY_LOCATION.distanceSquaredTo(target)) {
                        if (cosAngle > 0) {
                            bugpathTermCount++;
                            if (bugpathTermCount > 1) {
                                bugpathBlocked = false;
                                bugpathTurnCount = 0;
                            }
                        } else {
                            bugpathTermCount = 0;
                        }
                    } else {
                        bugpathTermCount = 0;
                    }
                    return true;
                }
            }
        }
        return false;
    }
}