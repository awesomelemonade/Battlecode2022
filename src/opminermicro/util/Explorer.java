package opminermicro.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static opminermicro.util.Constants.rc;

public class Explorer {
    private static Direction previousDirection = Util.randomAdjacentDirection();

    public static ExploreDirection currentExploreDirection = null;
    public static MapLocation currentExploreLocation;

    public static void init() {
        if (!Constants.ROBOT_TYPE.isBuilding() && rc.getRoundNum() < 100) {
            RobotInfo archon = Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> r.type == RobotType.ARCHON);
            if (archon != null) {
                currentExploreDirection = getInitialExploreDirection();
                currentExploreLocation = getExploreLocation();
            }
        }
    }

    public static ExploreDirection getInitialExploreDirection() {
        ExploreDirection[] allDirections = ExploreDirection.values();
        double[] scores = new double[allDirections.length];
        double sum = 0;
        for (int i = allDirections.length; --i >= 0;) {
            double score = getScore(allDirections[i]);
            sum += score;
            scores[i] = score;
        }
        double random = Math.random() * sum;
        for (int i = allDirections.length; --i >= 0;) {
            random -= scores[i];
            if (random < 0) {
                return allDirections[i];
            }
        }
        // Should never happen
        return Util.random(allDirections);
    }

    public static double getScore(ExploreDirection direction) {
        double dx = direction.dx;
        double dy = direction.dy;
        double hypot = Math.hypot(dx, dy);
        dx /= hypot;
        dy /= hypot;
        if (dx < 0) {
            // Going towards left border
            if (dy < 0) {
                return -Math.max(Cache.MY_LOCATION.x / dx, Cache.MY_LOCATION.y / dy);
            } else {
                return Math.min(-Cache.MY_LOCATION.x / dx, (Constants.MAP_HEIGHT - 1 - Cache.MY_LOCATION.y) / dy);
            }
        } else {
            if (dy < 0) {
                return Math.min((Constants.MAP_WIDTH - 1 - Cache.MY_LOCATION.x) / dx, -Cache.MY_LOCATION.y / dy);
            } else {
                return Math.min((Constants.MAP_WIDTH - 1 - Cache.MY_LOCATION.x) / dx, (Constants.MAP_HEIGHT - 1 - Cache.MY_LOCATION.y) / dy);
            }
        }
    }

    public static boolean randomExplore() {
        Debug.setIndicatorDot(Profile.EXPLORER, Cache.MY_LOCATION, 255, 128, 0); // orange
        Direction bestDirection = null;
        int minAllies = Integer.MAX_VALUE;
        for (Direction direction : Constants.getAttemptOrder(previousDirection)) {
            if (rc.canMove(direction)) {
                MapLocation next = Cache.MY_LOCATION.add(direction);
                int numAllies = Util.numAllyRobotsWithin(next, 10);
                if (numAllies < minAllies) {
                    bestDirection = direction;
                    minAllies = numAllies;
                }
            }
        }
        if (bestDirection != null) {
            Util.move(bestDirection);
            previousDirection = bestDirection;
            return true;
        }
        // traffic jam
        return false;
    }


    public static boolean smartExplore() {
        Debug.setIndicatorDot(Profile.EXPLORER, Cache.MY_LOCATION, 255, 128, 0); // orange
        if (currentExploreDirection == null || reachedBorder(currentExploreDirection)) {
            boolean noNewDirection = true;
            for (int i = 5; --i >= 0;) { // Only attempt 5 times
                ExploreDirection potentialDirection = Util.random(ExploreDirection.values());
                // checks that the potentialDirection is not in the same or opposite direction as exploreDirection
                if (currentExploreDirection != null && (currentExploreDirection == potentialDirection || ExploreDirection.isOpposite(currentExploreDirection, potentialDirection))) {
                    continue;
                }
                if (reachedBorder(potentialDirection)) {
                    continue;
                }
                currentExploreDirection = potentialDirection;
                noNewDirection = false;
                break;
            }
            if (noNewDirection) {
                currentExploreDirection = null;
            }
        }
        if (currentExploreDirection == null) {
            return randomExplore();
        } else {
            currentExploreLocation = getExploreLocation();
            Debug.setIndicatorLine(Profile.EXPLORER, Cache.MY_LOCATION, Cache.MY_LOCATION.translate(currentExploreDirection.dx, currentExploreDirection.dy), 255, 128, 0);
            return Pathfinder.execute(currentExploreLocation);
        }
    }

    public static MapLocation getExploreLocation() {
        return rc.getLocation().translate(currentExploreDirection.dx * 10, currentExploreDirection.dy * 10);
    }

    public static boolean goingTowardsAllyArchon(ExploreDirection direction) {
        MapLocation currentLocation = rc.getLocation();
        MapLocation exploreLocation = currentLocation.translate(direction.dx, direction.dy);
        int futureDist = MapInfo.getClosestAllyArchonDistanceSquared(exploreLocation, 1024);
        int curDist = MapInfo.getClosestAllyArchonDistanceSquared(currentLocation, 1024);
        return futureDist <= curDist && futureDist <= 25;
    }

    public static boolean reachedBorder(ExploreDirection direction) {
        return !Util.onTheMap(rc.getLocation().translate(direction.sigDx * 3, direction.sigDy * 3));
    }
}
