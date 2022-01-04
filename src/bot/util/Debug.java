package bot.util;

import battlecode.common.MapLocation;

import static bot.util.Constants.rc;
import static bot.util.Constants.CURRENT_PROFILE;

public class Debug {
    public static void println(String line) {
        System.out.println(line);
    }

    public static void setIndicatorString(String string) {
        rc.setIndicatorString(string);
    }

    public static void setIndicatorDot(MapLocation location, int red, int green, int blue) {
        rc.setIndicatorDot(location, red, green, blue);
    }

    public static void setIndicatorLine(MapLocation a, MapLocation b, int red, int green, int blue) {
        rc.setIndicatorLine(a, b, red, green, blue);
    }

    public static void println(Profile profile, String line) {
        if (profile == CURRENT_PROFILE) {
            System.out.println(line);
        }
    }

    public static void setIndicatorString(Profile profile, String string) {
        if (profile == CURRENT_PROFILE) {
            rc.setIndicatorString(string);
        }
    }

    public static void setIndicatorDot(Profile profile, MapLocation location, int red, int green, int blue) {
        if (profile == CURRENT_PROFILE) {
            rc.setIndicatorDot(location, red, green, blue);
        }
    }

    public static void setIndicatorLine(Profile profile, MapLocation a, MapLocation b, int red, int green, int blue) {
        if (profile == CURRENT_PROFILE) {
            rc.setIndicatorLine(a, b, red, green, blue);
        }
    }
}
