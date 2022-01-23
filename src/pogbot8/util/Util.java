package pogbot8.util;

import battlecode.common.*;

import java.util.function.Predicate;

import static pogbot8.util.Constants.ALL_DIRECTIONS;
import static pogbot8.util.Constants.rc;

public class Util {

    public static void init(RobotController controller) throws GameActionException {
        Constants.init(controller);
        Cache.init();
        Communication.init();
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
        double curDist2 = Cache.MY_LOCATION.distanceSquaredTo(location);
        double curDist = Math.sqrt(curDist2);
        Direction bestDir = null;
        for (Direction dir : ALL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(dir);
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
        if (bestDir != null && bestDir != Direction.CENTER) {
            tryMove(bestDir);
        }
    }

    public static void tryKiteFromGreedy(MapLocation location) throws GameActionException {
        double bestScore = 0;
        double curDist2 = Cache.MY_LOCATION.distanceSquaredTo(location);
        double curDist = Math.sqrt(curDist2);
        Direction bestDir = null;
        for (Direction dir : Constants.ORDINAL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(dir);
            if (rc.canMove(dir)) {
                int dist2 = loc.distanceSquaredTo(location);
                if (dist2 <= curDist2) continue;
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
        if (bestDir != null) {
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

    public static int getNextVortexOrSingularity() {
        int currentRound = rc.getRoundNum();
        AnomalyScheduleEntry[] schedule = rc.getAnomalySchedule();
        int ret = 2000;
        for (int i = Math.min(20, schedule.length); --i >= 0;) {
            AnomalyScheduleEntry entry = schedule[i];
            if (entry.anomalyType == AnomalyType.VORTEX && entry.roundNumber >= currentRound) {
                ret = Math.min(ret, entry.roundNumber);
            }
        }
        return ret;
    }

    // Attacker related
    public static MapLocation getRetreatLocation() throws GameActionException {
        if (rc.getHealth() >= rc.getType().getMaxHealth(rc.getLevel())) {
            return null;
        }
        int healthThreshold = (int) Math.round(Math.max(15.0, Math.min(30.0, 40.0 - 0.1 * Communication.getSoldierCombinedHealth())));
        if (rc.getHealth() <= healthThreshold) {
            return idealThenNearestVisible();
        }
        MapLocation location = nearestVisibleThenIdeal();
        return location != null && Cache.MY_LOCATION.isWithinDistanceSquared(location, 34) ? location : null;
    }

    private static MapLocation idealThenNearestVisible() {
        MapLocation location = getIdealAllyArchonForHeal();
        return location == null ? getNearestVisibleAllyArchonForHeal() : location;
    }

    private static MapLocation nearestVisibleThenIdeal() {
        MapLocation location = getNearestVisibleAllyArchonForHeal();
        return location == null ? getIdealAllyArchonForHeal() : location;
    }

    private static MapLocation getIdealAllyArchonForHeal() {
        double bestScore = Double.MAX_VALUE;
        MapLocation bestLocation = null;
        for (int i = Communication.archonLocations.length; --i >= 0; ) {
            MapLocation loc = Communication.archonLocations[i];
            if (loc == null) continue;
            if (Communication.archonPortable[i]) continue;
            double health = Communication.archonRepairAmounts[i];
            double score = health / 3.0 + 2.0 * Math.sqrt(loc.distanceSquaredTo(Cache.MY_LOCATION));
            if (score < bestScore) {
                bestScore = score;
                bestLocation = loc;
            }
        }
        return bestLocation;
    }

    public static MapLocation getNearestVisibleAllyArchonForHeal() {
        int bestDistanceSquared = Integer.MAX_VALUE;
        MapLocation bestMapLocation = null;
        for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
            RobotInfo robot = Cache.ALLY_ROBOTS[i];
            if (robot.type == RobotType.ARCHON && robot.mode == RobotMode.TURRET) {
                MapLocation location = robot.location;
                int distanceSquared = location.distanceSquaredTo(Cache.MY_LOCATION);
                if (distanceSquared < bestDistanceSquared) {
                    bestDistanceSquared = distanceSquared;
                    bestMapLocation = location;
                }
            }
        }
        return bestMapLocation;
    }

    private static MapLocation predictedArchonLocation = null;
    public static void tryMoveTowardsEnemyChunkOrExplore() throws GameActionException {
        MapLocation location = Communication.getClosestEnemyChunk();
        if (location == null) {
            if (predictedArchonLocation == null || Communication.getChunkInfo(predictedArchonLocation) != Communication.CHUNK_INFO_ENEMY_PREDICTED) {
                predictedArchonLocation = Communication.getRandomPredictedArchonLocation();
            }
            location = predictedArchonLocation;
        }
        if (location == null) {
            Util.tryExplore();
        } else {
            Debug.setIndicatorDot(Profile.ATTACKING, Cache.MY_LOCATION, 255, 255, 0);
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, location, 255, 255, 0);
            Util.tryMove(location);
        }
    }

    public static void tryMoveAttackingSquare(MapLocation location, int range) throws GameActionException {
        double bestScore = 0;
        Direction bestDir = null;
        for (Direction dir : Constants.ALL_DIRECTIONS) {
            MapLocation loc = Cache.MY_LOCATION.add(dir);
            if (dir == Direction.CENTER || rc.canMove(dir)) {
                int dist = loc.distanceSquaredTo(location);
                double distScore = 0.5 + dist / (2.0 * range);
                if (dist > range) distScore = 0.75 - dist / (2.0 * range);
                double cooldown = 1.0 + rc.senseRubble(loc) / 10.0;
                double cdScore = 1.0 / cooldown;
                double score = distScore + 10 * cdScore;
                if (score > bestScore) {
                    bestScore = score;
                    bestDir = dir;
                }
            }
        }
        if (bestDir != null && bestDir != Direction.CENTER) {
            Util.tryMove(bestDir);
        }
    }

    public static void tryMoveAttacker() throws GameActionException {
        MapLocation retreatLocation = Util.getRetreatLocation();
        if (retreatLocation != null) {
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, retreatLocation, 128, 128, 255);
            if (!Cache.MY_LOCATION.isWithinDistanceSquared(retreatLocation, 10)) {
                Util.tryMove(retreatLocation);
                return;
            }
        }
        RobotInfo closestEnemyAttacker = Util.getClosestEnemyRobot(r -> Util.isAttacker(r.type));
        if (closestEnemyAttacker != null) {
            if (rc.isActionReady()) {
                Util.tryMoveAttackingSquare(closestEnemyAttacker.location, Constants.ROBOT_TYPE.actionRadiusSquared);
            } else {
                if (Constants.ROBOT_TYPE == RobotType.SAGE) {
                    Util.tryKiteFromGreedy(closestEnemyAttacker.location);
                } else {
                    Util.tryKiteFrom(closestEnemyAttacker.location);
                }
            }
        } else {
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            if (closestEnemy != null) {
                Util.tryMoveAttackingSquare(closestEnemy.location, Constants.ROBOT_TYPE.actionRadiusSquared);
            } else {
                Util.tryMoveTowardsEnemyChunkOrExplore();
            }
        }
    }
}