package passivetest.util;

import battlecode.common.*;

import java.util.function.Predicate;

import static passivetest.util.Constants.rc;

public class Util {

    public static void init(RobotController controller) throws GameActionException {
        Constants.init(controller);
        Cache.init();
        Communication.init();
        MapInfo.init();
        Explorer.init();
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

    public static <T> T random(T[] array) {
        return array[(int) (Math.random() * array.length)];
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
            Cache.recalculate();
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
        return Pathfinder.execute(loc);
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

    public static void tryKiteFrom(MapLocation location) throws GameActionException {
        double bestScore = 0;
        double curDist2 = rc.getLocation().distanceSquaredTo(location);
        double curDist = Math.sqrt(curDist2);
        Direction bestDir = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation loc = rc.getLocation().translate(dx, dy);
                Direction dir = rc.getLocation().directionTo(loc);
                if (dir == Direction.CENTER || rc.canMove(dir)) {
                    int dist2 = loc.distanceSquaredTo(location);
                    if (dist2 < curDist2) continue;
                    double dist = Math.sqrt(dist2);
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
            tryMove(bestDir);
        }
    }

    public static boolean tryExplore() throws GameActionException {
        return Explorer.smartExplore();
    }

    public static int numAllyRobotsWithin(MapLocation location, int distanceSquared) {
        if (Cache.ALLY_ROBOTS.length >= 20) {
            return rc.senseNearbyRobots(location, distanceSquared, Constants.ALLY_TEAM).length;
        } else {
            // loop through robot list
            int count = 0;
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                if (location.isWithinDistanceSquared(Cache.ALLY_ROBOTS[i].getLocation(), distanceSquared)) {
                    count++;
                }
            }
            return count;
        }
    }

    public static <T> void shuffle(T[] array) {
        for (int i = array.length; --i >= 0;) {
            int index = (int) (Math.random() * i);
            T temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }

    public static boolean onTheMap(MapLocation location) {
        int x = location.x;
        int y = location.y;
        return x >= 0 && y >= 0 && x < Constants.MAP_WIDTH && y < Constants.MAP_HEIGHT;
    }
}