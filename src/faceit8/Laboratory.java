package faceit8;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotMode;
import battlecode.common.RobotType;
import faceit8.util.*;

import static faceit8.util.Constants.ALLY_TEAM;
import static faceit8.util.Constants.rc;

public class Laboratory implements RunnableBot {
    private static int turnsStuck = 0;
    @Override
    public void init() throws GameActionException {

    }

    public static boolean shouldTransformToPortable() throws GameActionException {
        if (!rc.canTransform()) {
            return false;
        }
        if (Util.getNextVortexOrSingularity() == rc.getRoundNum()) {
            return true;
        }
        MapLocation potentialRelocationTarget = getTargetMoveLocation();
        if (potentialRelocationTarget != null && !Cache.MY_LOCATION.equals(potentialRelocationTarget) && isWorthToMove(potentialRelocationTarget)) {
            return true;
        }
        return false;
    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getMode() == RobotMode.TURRET) {
            if (shouldTransformToPortable()) {
                rc.transform();
                turnsStuck = 0;
            }
            int reservedLead = Communication.getReservedLead();
            if (reservedLead != RobotType.MINER.buildCostLead) { // Laboratories only respect miner reservations
                reservedLead = 0;
            }
            if (rc.getTeamLeadAmount(ALLY_TEAM) - rc.getTransmutationRate() >= reservedLead) {
                if (rc.canTransmute()) {
                    rc.transmute();
                }
            } else {
                Debug.setIndicatorDot(Cache.MY_LOCATION, 255, 0, 0);
            }
        } else if (rc.getMode() == RobotMode.PORTABLE) {
            MapLocation relocationTarget = getTargetMoveLocation();
            if (relocationTarget == null) {
                // wtf???
                Debug.println("No relocation target??");
                relocationTarget = Cache.MY_LOCATION;
            }
            if (Cache.MY_LOCATION.equals(relocationTarget) || turnsStuck >= 20) {
                if (rc.canTransform()) {
                    rc.transform();
                }
            } else {
                if (rc.isMovementReady()) {
                    turnsStuck++;
                }
                Util.tryMove(relocationTarget);
            }
        }
    }

    public static boolean isWorthToMove(MapLocation location) throws GameActionException {
        if (Cache.TURN_COUNT < 100) { // TODO: Temporary
            return false;
        }
        int currentRubble = rc.senseRubble(Cache.MY_LOCATION);
        int destinationRubble = rc.senseRubble(location);
        double currentCooldown = 1.0 + currentRubble / 10.0; // turns / unit
        double currentCooldownTurns = currentCooldown * 2.4; // turns
        double destinationCooldown = (1.0 + destinationRubble / 10.0);
        double destinationCooldownTurns = destinationCooldown * 2.4;
        double averageCooldownTurns = (currentCooldownTurns + destinationCooldownTurns) / 2.0;
        double turnsToDestination = Math.sqrt(Cache.MY_LOCATION.distanceSquaredTo(location)) * averageCooldownTurns;
        double totalTurns = turnsToDestination + 10.0 * currentCooldown + 10.0 * destinationCooldown; // turns
        double transmutationsMissed = totalTurns / currentCooldown; // units
        double catchUpRate = 1.0 / destinationCooldown - 1.0 / currentCooldown; // number of more units per turn (units / turn)
        double payoffTurns = transmutationsMissed / catchUpRate + totalTurns;
        return 5.0 + 1.25 * payoffTurns < Util.getNextVortexOrSingularity() - rc.getRoundNum();
    }

    public static MapLocation getTargetMoveLocation() throws GameActionException {
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(Cache.MY_LOCATION, 13);
        MapLocation bestLocation = null;
        int bestRubble = Integer.MAX_VALUE;
        int bestDistanceSquared = 0;
        for (int i = locations.length; --i >= 0;) {
            MapLocation location = locations[i];
            if (rc.onTheMap(location)) {
                if (location.equals(Cache.MY_LOCATION) || !rc.isLocationOccupied(location)) {
                    int rubble = rc.senseRubble(location);
                    int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(location);
                    if (rubble < bestRubble || rubble == bestRubble && distanceSquared < bestDistanceSquared) {
                        bestLocation = location;
                        bestRubble = rubble;
                        bestDistanceSquared = distanceSquared;
                    }
                }
            }
        }
        return bestLocation;
    }
}
