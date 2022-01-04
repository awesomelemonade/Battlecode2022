package bot.util;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

public class Constants {
    // TODO: CHECK BEFORE SUBMITTING
    public static final boolean ENABLE_DEBUG = true;
    public static final Profile CURRENT_PROFILE = Profile.MAIN;
    public static final boolean DEBUG_DRAW = true;
    public static final boolean DEBUG_RESIGN = false;
    public static final boolean DEBUG_PRINT = false;

    public static Team ALLY_TEAM;
    public static Team ENEMY_TEAM;
    public static final int MAX_MAP_SIZE = 60;
    public static final int MAX_DISTANCE_SQUARED = (MAX_MAP_SIZE - 1) * (MAX_MAP_SIZE - 1);
    public static MapLocation SPAWN;

    public static RobotController rc;

    public static void init(RobotController controller) {
        Constants.rc = controller;
        ALLY_TEAM = controller.getTeam();
        ENEMY_TEAM = ALLY_TEAM.opponent();
        SPAWN = controller.getLocation();
    }

    public static final Direction[] CARDINAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };

    public static final Direction[] ORDINAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    // TODO: Was this used for pathfinding?
    //public static final int[] ORDINAL_OFFSET_X = {0, 1, 1, 1, 0, -1, -1, -1, 1, 2, 2, 1, -1, -2, -2, -1};
    //public static final int[] ORDINAL_OFFSET_Y = {-1, -1, 0, 1, 1, 1, 0, -1, 2, 1, -1, -2, -2, -1, 1, 2};

    private static final Direction[][] ATTEMPT_ORDER = new Direction[][] {
            // NORTH
            {Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST, Direction.WEST, Direction.EAST, Direction.SOUTHWEST, Direction.SOUTHEAST, Direction.SOUTH},
            // NORTHEAST
            {Direction.NORTHEAST, Direction.NORTH, Direction.EAST, Direction.NORTHWEST, Direction.SOUTHEAST, Direction.WEST, Direction.SOUTH, Direction.SOUTHWEST},
            // EAST
            {Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST, Direction.NORTH, Direction.SOUTH, Direction.NORTHWEST, Direction.SOUTHWEST, Direction.WEST},
            // SOUTHEAST
            {Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH, Direction.NORTHEAST, Direction.SOUTHWEST, Direction.NORTH, Direction.WEST, Direction.NORTHWEST},
            // SOUTH
            {Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.EAST, Direction.WEST, Direction.NORTHEAST, Direction.NORTHWEST, Direction.NORTH},
            // SOUTHWEST
            {Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST, Direction.SOUTHEAST, Direction.NORTHWEST, Direction.EAST, Direction.NORTH, Direction.NORTHEAST},
            // WEST
            {Direction.WEST, Direction.SOUTHWEST, Direction.NORTHWEST, Direction.SOUTH, Direction.NORTH, Direction.SOUTHEAST, Direction.NORTHEAST, Direction.EAST},
            // NORTHWEST
            {Direction.NORTHWEST, Direction.WEST, Direction.NORTH, Direction.SOUTHWEST, Direction.NORTHEAST, Direction.SOUTH, Direction.EAST, Direction.SOUTHEAST},
    };

    public static Direction[] getAttemptOrder(Direction direction) {
        return ATTEMPT_ORDER[direction.ordinal()];
    }
}
