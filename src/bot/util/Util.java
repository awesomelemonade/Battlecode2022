package bot.util;

import battlecode.common.*;

import java.util.function.Predicate;

import static bot.util.Constants.rc;

public class Util {

    public static void init(RobotController controller) throws GameActionException {
        Constants.init(controller);
        Cache.init();
        Communication.init();
        MapInfo.init();
    }

    public static void loop() throws GameActionException {
        Cache.loop();
        Communication.loop();
    }

    public static void postLoop() throws GameActionException {
        Communication.postLoop();
    }

    public static Direction random(Direction[] directions) {
        return directions[(int) (Math.random() * directions.length)];
    }

    public static Direction randomAdjacentDirection() {
        return random(Constants.ORDINAL_DIRECTIONS);
    }

    public static boolean isAttacker(RobotType type) {
        switch (type) {
            case SOLDIER:
            case SAGE:
            case WATCHTOWER:
                return true;
            default:
                return false;
        }
    }

    public static RobotInfo getClosestRobot(RobotInfo[] robots, Predicate<RobotInfo> filter) {
        int bestDistanceSquared = Integer.MAX_VALUE;
        RobotInfo bestRobot = null;
        for (RobotInfo robot : robots) {
            if (filter.test(robot)) {
                int distanceSquared = robot.getLocation().distanceSquaredTo(rc.getLocation());
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestRobot = robot;
                }
            }
        }
        return bestRobot;
    }

    public static RobotInfo getClosestEnemyRobot(MapLocation location, int limit, Predicate<RobotInfo> filter) {
        int bestDistanceSquared = limit + 1;
        RobotInfo bestRobot = null;
        for (RobotInfo enemy : Cache.ENEMY_ROBOTS) {
            if (filter.test(enemy)) {
                int distanceSquared = enemy.getLocation().distanceSquaredTo(location);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestRobot = enemy;
                }
            }
        }
        return bestRobot;
    }

    public static RobotInfo getClosestEnemyRobot(Predicate<RobotInfo> filter) {
        return getClosestEnemyRobot(rc.getLocation(), Constants.MAX_DISTANCE_SQUARED, filter);
    }

    public static RobotInfo getClosestEnemyRobot() {
        int bestDistanceSquared = Integer.MAX_VALUE;
        RobotInfo bestRobot = null;
        for (int i = Cache.ENEMY_ROBOTS.length; --i >= 0;) {
            RobotInfo enemy = Cache.ENEMY_ROBOTS[i];
            int distanceSquared = enemy.getLocation().distanceSquaredTo(rc.getLocation());
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestRobot = enemy;
            }
        }
        return bestRobot;
    }

    public static void move(Direction direction) {
        try {
            rc.move(direction);
        } catch (GameActionException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static boolean tryMove(Direction direction) {
        if (rc.canMove(direction)) {
            move(direction);
            return true;
        } else {
            return false;
        }
    }

    public static boolean tryMove(MapLocation loc) {
        // REPLACE THIS WITH PATHFINDER STUFF WHEN THAT IS IMPLEMENTED.
        if (loc.equals(rc.getLocation())) return false;
        return tryMoveTowards(rc.getLocation().directionTo(loc));
    }

    public static boolean tryMoveTowards(Direction direction) {
        for (Direction moveDirection : Constants.getAttemptOrder(direction)) {
            if (tryMove(moveDirection)) {
                return true;
            }
        }
        return false;
    }

    public static boolean tryRandomMove() throws GameActionException {
        return tryMove(randomAdjacentDirection());
    }

    public static boolean tryKiteFrom(MapLocation location) throws GameActionException {
        return Util.tryMoveTowards(location.directionTo(rc.getLocation()));
    }
}
