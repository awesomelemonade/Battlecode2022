package fixedmicro.util;

import battlecode.common.MapLocation;

public class MapInfo {
    // TODO: Predict symmetry - Horizontal, Vertical, Rotational
    public static MapLocation[] INITIAL_ARCHON_LOCATIONS;

    public static void init() {
        // Communication has to be initialized beforehand

        // Assume all symmetries are possible

    }

    public static int getClosestAllyArchonDistanceSquared(MapLocation location, int searchDistance) {
        int bestDistanceSquared = searchDistance;
        for (int i = INITIAL_ARCHON_LOCATIONS.length; --i >= 0;) {
            MapLocation archonLocation = INITIAL_ARCHON_LOCATIONS[i];
            int distanceSquared = location.distanceSquaredTo(archonLocation);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
            }
        }
        return bestDistanceSquared;
    }
}
