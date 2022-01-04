package bot.util;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

import static bot.util.Constants.controller;

public class Util {

    public static void init(RobotController controller) {
        Constants.init(controller);
        Cache.init();
    }

    public static void loop() throws GameActionException {
        Cache.loop();
    }

    public static void postLoop() throws GameActionException {
        // TODO
    }
}
