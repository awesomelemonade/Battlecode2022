package realhealingcomms.util;

import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import static realhealingcomms.util.Constants.rc;

public class Cache { // Cache variables that are constant throughout a turn
    public static RobotInfo[] ALLY_ROBOTS, ENEMY_ROBOTS;
    public static int TURN_COUNT;
    public static MapLocation MY_LOCATION;

    public static void init() {
        TURN_COUNT = 0;
        ALLY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ALLY_TEAM);
        ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
        MY_LOCATION = rc.getLocation();
    }

    public static void loop() {
        if (TURN_COUNT > 0) {
            ALLY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ALLY_TEAM);
            ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
            MY_LOCATION = rc.getLocation();
        }
        TURN_COUNT++;
    }

    public static void recalculate() {
        ALLY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ALLY_TEAM);
        ENEMY_ROBOTS = rc.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
        MY_LOCATION = rc.getLocation();
    }
}