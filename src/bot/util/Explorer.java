package bot.util;

import battlecode.common.*;

import static bot.util.Constants.rc;

public class Explorer {
    private static Direction initialExploreDirection = Util.randomAdjacentDirection(); // Buildings can technically explore
    private static Direction previousDirection = Util.randomAdjacentDirection();

    public static void init() {
        if (!Constants.ROBOT_TYPE.isBuilding()) {
            RobotInfo archon = Util.getClosestRobot(Cache.ALLY_ROBOTS, r -> r.type == RobotType.ARCHON);
            if (archon != null) {
                initialExploreDirection = archon.location.directionTo(rc.getLocation());
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

    private static ExploreDirection currentExploreDirection = null;
    private static boolean hasExplored = false;
    public static MapLocation currentExploreLocation;

    public static boolean smartExplore() {
        Debug.setIndicatorDot(Profile.EXPLORER, Cache.MY_LOCATION, 255, 128, 0); // orange
        // TODO: Simplify
        if (currentExploreDirection == null || reachedBorder(currentExploreDirection) || goingTowardsAllyArchon(currentExploreDirection)) {
            if (rc.getRoundNum() < 100 && !hasExplored) {
                currentExploreDirection = ExploreDirection.fromDirection(initialExploreDirection);
                currentExploreLocation = getExploreLocation();
                hasExplored = true;
            } else {
                // shuffle directions
                currentExploreDirection = null;
                for (int i = 5; --i >= 0;) { // Only attempt 5 times
                    ExploreDirection potentialDirection = Util.random(ExploreDirection.values());
                    // checks that the potentialDirection is not in the same or opposite direction as exploreDirection
                    if (currentExploreDirection != null && (currentExploreDirection == potentialDirection || ExploreDirection.isOpposite(currentExploreDirection, potentialDirection))) {
                        continue;
                    }
                    if (reachedBorder(potentialDirection) || goingTowardsAllyArchon(potentialDirection)) {
                        continue;
                    }
                    currentExploreDirection = potentialDirection;
                    currentExploreLocation = getExploreLocation();
                    break;
                }
            }
        }
        if (currentExploreDirection == null) {
            return randomExplore();
        } else {
            return Pathfinder.execute(currentExploreLocation);
        }
    }

    public static MapLocation getExploreLocation() {
        return rc.getLocation().translate(currentExploreDirection.dx * Constants.MAP_WIDTH, currentExploreDirection.dy * Constants.MAP_HEIGHT);
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
