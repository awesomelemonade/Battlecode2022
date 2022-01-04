package bot;

import battlecode.common.*;
import bot.util.*;

public class RobotPlayer {
    public static int currentTurn;
    public static void run(RobotController controller) throws GameActionException {
        Constants.controller = controller;

        RobotType robotType = controller.getType();
        RunnableBot bot;
        switch (robotType) { // Can't use switch expressions :(
            case ARCHON:
                bot = new Archon();
                break;
            case SOLDIER:
                bot = new Soldier();
                break;
            default:
                throw new IllegalStateException("Unknown Robot Type: " + robotType);
        }

        try {
            bot.init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        boolean errored = false;
        boolean overBytecodes = false;

        // Battlecodify
        while (true) {
            try {
                while (true) {
                    currentTurn = controller.getRoundNum();
                    if (Constants.DEBUG_RESIGN && (currentTurn >= 800 || currentTurn >= 350 && controller.getRobotCount() < 10)) {
                        controller.resign();
                    }
                    Util.loop();
                    bot.loop();
                    Util.postLoop();
                    if (controller.getRoundNum() != currentTurn) {
                        overBytecodes = true;
                        // We ran out of bytecodes! - MAGENTA
                        Debug.setIndicatorDot(controller.getLocation(), 255, 0, 255);
                        int over = Clock.getBytecodeNum() + (controller.getRoundNum() - currentTurn - 1) * controller.getType().bytecodeLimit;
                        Debug.println(controller.getLocation() + " out of bytecodes: " + Cache.TURN_COUNT + " (over by " + over + ")");
                    }
                    if (errored) {
                        Debug.setIndicatorDot(controller.getLocation(), 255, 0, 0); // red
                    }
                    if (overBytecodes) {
                        Debug.setIndicatorDot(controller.getLocation(), 128, 0, 255); // purple
                    }
                    Clock.yield();
                }
            } catch (Exception ex) {
                Debug.println(controller.getLocation() + " errored: " + Cache.TURN_COUNT);
                ex.printStackTrace();
                errored = true;
                Clock.yield();
            }
        }
    }
}
