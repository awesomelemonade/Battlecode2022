package newbuildorder4.util;

import battlecode.common.*;

import static newbuildorder4.util.Constants.rc;

public class Explorer {
    private static Direction previousDirection = Util.randomAdjacentDirection();

    private static ExploreDirection currentExploreDirection = null;

    public static void init() {
        if (!Constants.ROBOT_TYPE.isBuilding()) {
            currentExploreDirection = getInitialExploreDirection();
        }
    }

    public static ExploreDirection getInitialExploreDirection() {
        int exploreX = (int)(Math.random() * Constants.MAP_WIDTH);
        int exploreY = (int)(Math.random() * Constants.MAP_HEIGHT);
        double dx = exploreX - Cache.MY_LOCATION.x;
        double dy = exploreY - Cache.MY_LOCATION.y;
        double angle = Math.atan2(dy, dx);
        double bestDiff = 1e9;
        ExploreDirection bestDir = null;
        for (ExploreDirection dir : ExploreDirection.values()) {
            double dirAngle = Math.atan2(dir.dy, dir.dx);
            double z = Math.abs(angle - dirAngle);
            double diff = Math.min(z, 2 * Math.PI - z);
            if (diff < bestDiff) {
                bestDiff = diff;
                bestDir = dir;
            }
        }
        return bestDir;
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
            Debug.setIndicatorLine(Profile.EXPLORER, Cache.MY_LOCATION, Cache.MY_LOCATION.translate(currentExploreDirection.dx, currentExploreDirection.dy), 255, 128, 0);
            return Pathfinder.execute(getExploreLocation());
        }
    }

    public static MapLocation getExploreLocation() {
        return rc.getLocation().translate(currentExploreDirection.dx * 10, currentExploreDirection.dy * 10);
    }

    public static boolean reachedBorder(ExploreDirection direction) {
        return !Util.onTheMap(rc.getLocation().translate(direction.dx * 2, direction.dy * 2));
    }
}
