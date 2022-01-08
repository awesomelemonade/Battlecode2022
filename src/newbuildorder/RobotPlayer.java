package newbuildorder;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotType;
import newbuildorder.util.*;

public class RobotPlayer {
    public static int currentTurn;
    public static void run(RobotController controller) throws GameActionException {
        Constants.rc = controller;

        RobotType robotType = controller.getType();
        RunnableBot bot;
        switch (robotType) { // Can't use switch expressions :(
            case ARCHON:
                bot = new Archon();
                break;
            case WATCHTOWER:
                bot = new Watchtower();
                break;
            case LABORATORY:
                bot = new Laboratory();
                break;
            case MINER:
                bot = new Miner();
                break;
            case BUILDER:
                bot = new Builder();
                break;
            case SOLDIER:
                bot = new Soldier();
                break;
            case SAGE:
                bot = new Sage();
                break;
            default:
                throw new IllegalStateException("Unknown Robot Type: " + robotType);
        }

        try {
            Util.init(controller);
            bot.init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        boolean errored = false;
        boolean overBytecodes = false;

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
                        // We ran out of bytecodes!
                        int over = Clock.getBytecodeNum() + (controller.getRoundNum() - currentTurn - 1) * controller.getType().bytecodeLimit;
                        Debug.println(Profile.ERROR_STATE, controller.getLocation() + " out of bytecodes: " + Cache.TURN_COUNT + " (over by " + over + ")");
                    }
                    if (errored) {
                        Debug.setIndicatorDot(Profile.ERROR_STATE, controller.getLocation(), 255, 0, 255); // pink
                    }
                    if (overBytecodes) {
                        Debug.setIndicatorDot(Profile.ERROR_STATE, controller.getLocation(), 128, 0, 255); // purple
                    }
                    Clock.yield();
                }
            } catch (Exception ex) {
                Debug.println(Profile.ERROR_STATE, controller.getLocation() + " errored: " + Cache.TURN_COUNT);
                ex.printStackTrace();
                errored = true;
                Clock.yield();
            }
        }
    }
}
