package goldnova;

import battlecode.common.*;
import goldnova.util.*;

import static goldnova.util.Constants.*;

public class Archon implements RunnableBot {
    private static double averageIncome;
    private static double averageIncomePerMiner;
    private static int wantedEarlygameMiners;
    private static int turnsStuck = 0;

    @Override
    public void init() throws GameActionException {
        wantedEarlygameMiners = Math.min(12, Math.max(4, MAP_WIDTH * MAP_HEIGHT / 200)) / rc.getArchonCount();
    }

    @Override
    public void loop() throws GameActionException {
        int numBuilders = Communication.getAliveRobotTypeCount(RobotType.BUILDER);
        int numMiners = Communication.getAliveRobotTypeCount(RobotType.MINER);
        int numSoldiers = Communication.getAliveRobotTypeCount(RobotType.SOLDIER);
        int numWatchtowers = Communication.getAliveRobotTypeCount(RobotType.WATCHTOWER);
        int soldierCombinedHealth = Communication.getSoldierCombinedHealth();
        if (numMiners == 0) {
            averageIncome = 0;
            averageIncomePerMiner = 0;
        } else {
            double ratio = 1.0 / 41.0;
            if (rc.getRoundNum() < 40) {
                ratio = 0.1;
            }
            averageIncome = ratio * Communication.getLeadIncome() + (1 - ratio) * averageIncome;
            averageIncomePerMiner = averageIncome / numMiners;
        }
        Debug.setIndicatorString("B: " + numBuilders + ", M: " + numMiners + ", S: " + numSoldiers + ", W: " + numWatchtowers + ", H: " + soldierCombinedHealth + ", I: " + averageIncome + ", I/M " + (averageIncome / numMiners));
        if (rc.getMode() == RobotMode.TURRET) {
            if (!Communication.hasPortableArchon()) {
                MapLocation potentialRelocationTarget = getTargetMoveLocation();
                if (potentialRelocationTarget != null && !Cache.MY_LOCATION.equals(potentialRelocationTarget) && isWorthToMove(potentialRelocationTarget)) {
                    if (rc.canTransform()) {
                        rc.transform();
                        Communication.setPortableArchon();
                        turnsStuck = 0;
                    }
                }
            }
            build:
            {
                if (tryBuildDefenders()) break build;
                if (tryBuildEarlygame()) break build;
                if (tryBuildLategame()) break build;
                if (tryBuildRich()) break build;
                if (tryBuildPoor()) break build;
            }
            tryRepair();
        } else {
            // Portable
            Communication.setPortableArchon();
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

    public static boolean tryBuildAttacker() throws GameActionException {
        if (rc.getTeamGoldAmount(ALLY_TEAM) >= RobotType.SAGE.buildCostGold) {
            return tryBuild(RobotType.SAGE);
        }
        return tryBuild(RobotType.SOLDIER);
    }

    public static boolean tryBuildDefenders() throws GameActionException {
        int sumEnemy = 0;
        for (RobotInfo robot : Cache.ENEMY_ROBOTS) {
            if (Util.isAttacker(robot.type)) {
                sumEnemy += robot.health;
            }
        }
        if (sumEnemy == 0) return false;
        int sumAlly = 0;
        for (RobotInfo robot : Cache.ALLY_ROBOTS) {
            if (Util.isAttacker(robot.type)) {
                sumAlly += robot.health;
            }
        }
        boolean beingAttacked = sumEnemy >= sumAlly;
        boolean winnable = 1.5 * sumAlly + 5 * RobotType.SOLDIER.health >= sumEnemy;
        if (beingAttacked) {
            if (winnable || rc.getArchonCount() == 1) {
                tryBuildAttacker();
            }
            return true;
        }
        return false;
    }

    public static boolean tryBuildEarlygame() throws GameActionException {
        if (wantedEarlygameMiners > 0) {
            if (tryBuild(RobotType.MINER)) {
                --wantedEarlygameMiners;
            }
            return true;
        } else {
            int numLaboratories = Communication.getAliveRobotTypeCount(RobotType.LABORATORY);
            int numBuilder = Communication.getAliveRobotTypeCount(RobotType.BUILDER);
            if (numLaboratories < 1 && numBuilder < 1) {
                tryBuild(RobotType.BUILDER);
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean tryBuildLategame() throws GameActionException {
        if (rc.getRoundNum() < 1900) return false;
        if (rc.getTeamLeadAmount(ALLY_TEAM) < 500) {
            if (averageIncome >= 5) {
                if (Communication.getAliveRobotTypeCount(RobotType.BUILDER) < 2) {
                    tryBuild(RobotType.BUILDER);
                }
            } else {
                tryBuildAttacker();
            }
        } else {
            if (Communication.getAliveRobotTypeCount(RobotType.BUILDER) < 2) {
                tryBuild(RobotType.BUILDER);
            }
        }
        return true;
    }

    public static void tryBuildRatio(double builder, double miner, double attacker) throws GameActionException {
        double prod = (builder <= 0 ? 1 : builder) * (miner <= 0 ? 1 : miner) * (attacker <= 0 ? 1 : attacker);
        double builderScore = builder <= 0 ? Integer.MAX_VALUE : Communication.getAliveRobotTypeCount(RobotType.BUILDER) * (prod/builder);
        double minerScore = miner <= 0 ? Integer.MAX_VALUE : Communication.getAliveRobotTypeCount(RobotType.MINER) * (prod/miner);
        double attackerScore = attacker <= 0 ? Integer.MAX_VALUE :
                (Communication.getSoldierCombinedHealth() / 50.0 +
                Communication.getAliveRobotTypeCount(RobotType.SAGE) +
                Communication.getAliveRobotTypeCount(RobotType.WATCHTOWER)) * (prod / attacker);
        if (minerScore < attackerScore && minerScore < builderScore && Communication.getAliveRobotTypeCount(RobotType.MINER) <= MAP_WIDTH * MAP_HEIGHT / 18) {
            tryBuild(RobotType.MINER);
        } else {
            if (attackerScore < builderScore) {
                tryBuildAttacker();
            } else {
                tryBuild(RobotType.BUILDER);
            }
        }
    }

    public static boolean tryBuildRich() throws GameActionException {
        if (rc.getTeamLeadAmount(ALLY_TEAM) < 500) return false;
        if (averageIncomePerMiner >= 0.6) {
            tryBuildRatio(1, 2, 6);
        } else {
            tryBuildRatio(1, 0, 6);
        }
        return true;
    }

    public static boolean tryBuildPoor() throws GameActionException {
        if (rc.getTeamLeadAmount(ALLY_TEAM) >= 500) return false;
        // 1:2 at 0.6, 1:1 at 1
        double ratio = Math.max(0.5, -2.5 * averageIncomePerMiner + 3.5);
        tryBuildRatio(0, 1, ratio);
        return true;
    }

    public static boolean tryBuild(RobotType type) throws GameActionException {
        int reservedLead = Communication.getReservedLead();
        int reservedGold = Communication.getReservedGold();

        if (reservedLead != 0 || reservedGold != 0) {
            // There already exists a reservation
            int remainingLead = rc.getTeamLeadAmount(ALLY_TEAM) - type.buildCostLead;
            int remainingGold = rc.getTeamGoldAmount(ALLY_TEAM) - type.buildCostGold;
            if (remainingLead < reservedLead || remainingGold < reservedGold) {
                return false;
            } else {
                for (Direction d: Constants.getAttemptOrder(Util.randomAdjacentDirection())) {
                    if (rc.canBuildRobot(type, d)) {
                        rc.buildRobot(type, d);
                        return true;
                    }
                }
                return false;
            }
        } else {
            // Build if we can, otherwise reserve
            Direction idealDirection = type == RobotType.MINER ? getIdealBuildDirectionForMining() : Util.randomAdjacentDirection();
            for (Direction d: Constants.getAttemptOrder(idealDirection)) {
                if (rc.canBuildRobot(type, d)) {
                    rc.buildRobot(type, d);
                    return true;
                }
            }
            Communication.reserve(type.buildCostGold, type.buildCostLead);
            return false;
        }
    }

    public static void tryRepair() throws GameActionException {
        if (!rc.isActionReady()) {
            return;
        }
        if (rc.getTeamLeadAmount(ALLY_TEAM) < 500 && rc.senseRubble(Cache.MY_LOCATION) > 20) {
            return; // We should probably be saving to build units
        }
        MapLocation bestLocation = null;
        int bestScore = Integer.MIN_VALUE;
        if (Cache.ENEMY_ROBOTS.length > 0) {
            // If we see any enemies
            // Repair the lowest health so they can survive
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                RobotInfo robot = Cache.ALLY_ROBOTS[i];
                MapLocation location = robot.location;
                if (!rc.canRepair(location)) {
                    continue;
                }
                int health = robot.health;
                int maxHealth = robot.type.health;
                if (health >= maxHealth) {
                    continue;
                }
                int score = (Util.isAttacker(robot.type) ? 1000 : 0) + maxHealth - health;
                if (score > bestScore) {
                    bestScore = score;
                    bestLocation = location;
                }
            }
        } else {
            // No enemies in sight
            // Repair the highest health that isn't full so they can leave (unclogging mechanism)
            for (int i = Cache.ALLY_ROBOTS.length; --i >= 0;) {
                RobotInfo robot = Cache.ALLY_ROBOTS[i];
                MapLocation location = robot.location;
                if (!rc.canRepair(location)) {
                    continue;
                }
                int health = robot.health;
                int maxHealth = robot.type.health;
                if (health >= maxHealth) {
                    continue;
                }
                int score = (Util.isAttacker(robot.type) ? 1000 : 0) + health;
                if (score > bestScore) {
                    bestScore = score;
                    bestLocation = location;
                }
            }
        }
        if (bestLocation != null) {
            Debug.setIndicatorLine(Profile.ATTACKING, Cache.MY_LOCATION, bestLocation, 0, 255, 0);
            rc.repair(bestLocation);
        }
    }

    public static Direction getIdealBuildDirectionForMining() throws GameActionException {
        MapLocation[] leadLocations = rc.senseNearbyLocationsWithLead(ARCHON_VISION_DISTANCE_SQUARED);
        MapLocation bestLocation = null;
        double bestScore = -Double.MAX_VALUE;
        if (leadLocations.length > 64) {
            // Search in restricted range to save bytecodes
            leadLocations = rc.senseNearbyLocationsWithLead(13);
        }
        for (int i = leadLocations.length; --i >= 0;) {
            MapLocation location = leadLocations[i];
            int lead = rc.senseLead(location);
            if (lead <= 6) {
                continue;
            }
            int dx = location.x - Cache.MY_LOCATION.x;
            int dy = location.y - Cache.MY_LOCATION.y;
            double distance = Math.sqrt(Cache.MY_LOCATION.distanceSquaredTo(location));
            double atan2 = Math.atan2(dy, dx);
            double score = (lead / 5.0 - distance) * (2.0 * Math.PI) + atan2;
            if (score > bestScore) {
                bestScore = score;
                bestLocation = location;
            }
        }
        if (bestLocation == null) {
            return Util.randomAdjacentDirection();
        } else {
            Direction ret = Generated34.execute(bestLocation);
            Debug.setIndicatorLine(Profile.MINING, Cache.MY_LOCATION, bestLocation, 255, 255, 0);
            if (ret == null || ret == Direction.CENTER) {
                return Util.randomAdjacentDirection();
            } else {
                return ret;
            }
        }
    }

    public static MapLocation getTargetMoveLocation() throws GameActionException {
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(Cache.MY_LOCATION, ROBOT_TYPE.visionRadiusSquared);
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

    public static boolean isWorthToMove(MapLocation location) throws GameActionException {
        if (rc.getRoundNum() < 20) {
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
        double unitsMissed = totalTurns / currentCooldown; // units
        double catchUpRate = 1.0 / destinationCooldown - 1.0 / currentCooldown; // number of more units per turn (units / turn)
        double payoffTurns = unitsMissed / catchUpRate + totalTurns;
        return 10.0 + 1.5 * payoffTurns < Util.getNextVortexOrSingularity() - rc.getRoundNum();
    }
}
