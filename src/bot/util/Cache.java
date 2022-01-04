package bot.util;

import battlecode.common.*;

import static bot.util.Constants.controller;

public class Cache { // Cache variables that are constant throughout a turn
    public static RobotInfo[] ALL_ROBOTS, ALLY_ROBOTS, ENEMY_ROBOTS, NEUTRAL_ROBOTS;
    public static RobotInfo[] EMPTY_ROBOTS = {};
    public static int TURN_COUNT;
    public static MapLocation MY_LOCATION;

    public static Direction lastDirection;
    public static void init() {
        TURN_COUNT = 0;
    }

    public static void loop() {
        lastDirection = Direction.CENTER;
        ALL_ROBOTS = controller.senseNearbyRobots();
        if (ALL_ROBOTS.length == 0) {
            // save 200 bytecodes
            ALLY_ROBOTS = EMPTY_ROBOTS;
            ENEMY_ROBOTS = EMPTY_ROBOTS;
            NEUTRAL_ROBOTS = EMPTY_ROBOTS;
        } else {
            ALLY_ROBOTS = controller.senseNearbyRobots(-1, Constants.ALLY_TEAM);
            ENEMY_ROBOTS = controller.senseNearbyRobots(-1, Constants.ENEMY_TEAM);
            NEUTRAL_ROBOTS = controller.senseNearbyRobots(-1, Team.NEUTRAL);
        }
        TURN_COUNT++;
        MY_LOCATION = controller.getLocation();
    }
}