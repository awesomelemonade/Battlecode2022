package retreater.util;

import battlecode.common.MapLocation;
import newbuildorder4.util.LambdaUtil;

public class MapInfo {
    // TODO: Predict symmetry - Horizontal, Vertical, Rotational
    public static MapLocation[] CURRENT_ARCHON_LOCATIONS;

    public static void init() {
        // Communication has to be initialized beforehand

        // Assume all symmetries are possible

    }

    public static MapLocation getClosestAllyArchonLocation() {
        int bestDist = (int)1e9;
        MapLocation bestLoc = null;
        for (int i = CURRENT_ARCHON_LOCATIONS.length; --i >= 0; ) {
            MapLocation loc = CURRENT_ARCHON_LOCATIONS[i];
            if (loc != null) {
                int dist = loc.distanceSquaredTo(Cache.MY_LOCATION);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestLoc = loc;
                }
            }
        }
        return bestLoc;
    }
}
