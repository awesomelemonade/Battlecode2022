package newbuildorder2;

import battlecode.common.*;
import newbuildorder2.util.*;

import static newbuildorder2.util.Constants.*;

public class Archon implements RunnableBot {
    private static MapLocation relocationTarget;

    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        int numArchons = Communication.getAliveRobotTypeCount(RobotType.ARCHON);
        int numMiners = Communication.getAliveRobotTypeCount(RobotType.MINER);
        int numSoldiers = Communication.getAliveRobotTypeCount(RobotType.SOLDIER);
        int numWatchtowers = Communication.getAliveRobotTypeCount(RobotType.WATCHTOWER);
        int numPassiveSoldiers = Communication.getPassiveSoldierCount();
        Debug.setIndicatorString("A: " + numArchons + ", M: " + numMiners + ", S: " + numSoldiers + ", W: " + numWatchtowers + ", P: " + numPassiveSoldiers);
        if (rc.getMode() == RobotMode.TURRET) {
            // TODO: build more miners based on lead communication
            if (!Communication.hasPortableArchon()) {
                MapLocation potentialRelocationTarget = getTargetMoveLocation();
                if (potentialRelocationTarget != null && isWorthToMove(potentialRelocationTarget)) {
                    relocationTarget = potentialRelocationTarget;
                    if (rc.canTransform()) {
                        rc.transform();
                        Communication.setPortableArchon();
                    }
                }
            }
            if (tryBuildDefenders()) return;
            if (tryBuildRich()) return;
            if (tryBuildPoor()) return;
            tryRepair();
        } else {
            // Portable
            Communication.setPortableArchon();
            if (relocationTarget == null) {
                // wtf???
                Debug.println("No relocation target??");
                relocationTarget = Cache.MY_LOCATION;
            }
            if (Cache.MY_LOCATION.equals(relocationTarget)) {
                if (rc.canTransform()) {
                    rc.transform();
                }
            } else {
                Util.tryMove(relocationTarget);
            }
        }

    }

    public boolean tryBuildAttacker() throws GameActionException {
        if (rc.getTeamGoldAmount(ALLY_TEAM) >= RobotType.SAGE.buildCostGold) {
            return tryBuild(RobotType.SAGE);
        }
        return tryBuild(RobotType.SOLDIER);
    }

    public boolean tryBuildDefenders() throws GameActionException {
        int numAllySoldiers = LambdaUtil.arraysStreamSum(Cache.ALLY_ROBOTS, r -> r.type == RobotType.SOLDIER || r.type == RobotType.SAGE);
        if (numAllySoldiers <= 5) { // We have sufficient defense
            RobotInfo closestEnemy = Util.getClosestEnemyRobot();
            if (closestEnemy != null && closestEnemy.location.isWithinDistanceSquared(rc.getLocation(), RobotType.ARCHON.visionRadiusSquared)) {
                tryBuildAttacker();
                return true;
            }
        }
        return false;
    }

    public boolean tryBuildRich() throws GameActionException {
        if (rc.getTeamLeadAmount(ALLY_TEAM) >= 500) {
            double r = Math.random();
            if (weAreMakingUselessSoldiers()) {
                if (r < 0.05) {
                    tryBuild(RobotType.MINER);
                } else {
                    int builderCount = Communication.getAliveRobotTypeCount(RobotType.BUILDER);
                    if (builderCount < 10 || Math.random() < 0.02) {
                        tryBuild(RobotType.BUILDER);
                    } else if (Math.random() < 0.1) {
                        tryBuildAttacker();
                    }
                }
            } else {
                if (r < 0.05) {
                    tryBuild(RobotType.MINER);
                } else if (Math.random() < 0.15) {
                    tryBuild(RobotType.BUILDER);
                } else {
                    tryBuildAttacker();
                }
            }
            return true;
        }
        return false;
    }

    public boolean tryBuildPoor() throws GameActionException {
        int mapSize = rc.getMapWidth() + rc.getMapHeight();
        boolean shouldBuildMiners = false;
        if (Cache.TURN_COUNT == 1) {
            shouldBuildMiners = true;
        }
        if (rc.getRoundNum() < 200) {
            if (Communication.getAliveRobotTypeCount(RobotType.MINER) <= 10 + mapSize / 20) {
                shouldBuildMiners = true;
            }
        } else {
            if (Communication.getAliveRobotTypeCount(RobotType.SOLDIER) <= 3) {
                if (Communication.getAliveRobotTypeCount(RobotType.MINER) <= 3 + mapSize / 20) {
                    shouldBuildMiners = true;
                }
            } else {
                if (Communication.getAliveRobotTypeCount(RobotType.MINER) <= 10 + mapSize / 20) {
                    shouldBuildMiners = true;
                }
            }
        }

        if (shouldBuildMiners) {
            tryBuild(RobotType.MINER);
        } else {
            double r = Math.random();
            if (r < 0.2 && rc.getTeamLeadAmount(rc.getTeam()) >= RobotType.SOLDIER.buildCostLead) { // TODO: Does not really use reservation system
                tryBuild(RobotType.MINER);
            } else {
                if (weAreMakingUselessSoldiers()) {
                    int builderCount = Communication.getAliveRobotTypeCount(RobotType.BUILDER);
                    int watchtowerCount = Communication.getAliveRobotTypeCount(RobotType.WATCHTOWER);
                    if (builderCount < 3 || Math.random() < 0.02) {
                        tryBuild(RobotType.BUILDER);
                    } else {
                        if (watchtowerCount < 3) {
                            if (Math.random() < 0.05) {
                                tryBuildAttacker();
                            }
                        } else {
                            if (Math.random() < 0.5) {
                                tryBuildAttacker();
                            }
                        }
                    }
                } else {
                    tryBuildAttacker();
                }
            }
        }
        return true;
    }

    public static boolean weAreMakingUselessSoldiers() {
        int numPassive = Communication.getPassiveSoldierCount();
        int totalSoldierCount = Communication.getAliveRobotTypeCount(RobotType.SOLDIER);
        if (totalSoldierCount <= 10) {
            return false;
        } else {
            double passivePercentage = ((double) numPassive) / ((double) totalSoldierCount);
            return passivePercentage > 0.45;
        }
    }

    public boolean tryBuild(RobotType type) throws GameActionException {
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
        double bestScore = -Double.MAX_VALUE;
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
                int maxHealth = robot.type.getMaxHealth(robot.level);
                if (health >= maxHealth) {
                    continue;
                }
                double score = health;
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
                int maxHealth = robot.type.getMaxHealth(robot.level);
                if (health >= maxHealth) {
                    continue;
                }
                double score = maxHealth - health;
                if (score > bestScore) {
                    bestScore = score;
                    bestLocation = location;
                }
            }
        }
        if (bestLocation != null) {
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
        MapLocation[] locations = rc.getAllLocationsWithinRadiusSquared(Cache.MY_LOCATION, RobotType.ARCHON.visionRadiusSquared);
        MapLocation bestLocation = null;
        int bestRubble = rc.senseRubble(Cache.MY_LOCATION);
        int bestDistanceSquared = 0;
        for (int i = locations.length; --i >= 0;) {
            MapLocation location = locations[i];
            if (rc.onTheMap(location)) {
                int rubble = rc.senseRubble(location);
                int distanceSquared = Cache.MY_LOCATION.distanceSquaredTo(location);
                if (rubble < bestRubble || rubble == bestRubble && distanceSquared < bestDistanceSquared) {
                    bestLocation = location;
                    bestRubble = rubble;
                    bestDistanceSquared = distanceSquared;
                }
            }
        }
        return bestLocation;
    }

    public static boolean isWorthToMove(MapLocation location) throws GameActionException {
        if (rc.getRoundNum() < 20 || rc.getArchonCount() <= 1) {
            return false;
        }
        int currentRubble = rc.senseRubble(Cache.MY_LOCATION);
        int destinationRubble = rc.senseRubble(location);
        double currentCooldown = 1.0 + currentRubble / 10.0; // turns / unit
        double currentCooldownTurns = currentCooldown * 2.4; // turns
        double destinationCooldown = (1.0 + destinationRubble / 10.0);
        double destinationCooldownTurns = destinationCooldown * 2.4;
        if (rc.getTeamLeadAmount(ALLY_TEAM) < 100 && destinationCooldown < currentCooldown * 2.0 / 3.0) {
            return true;
        }
        double averageCooldownTurns = (currentCooldownTurns + destinationCooldownTurns) / 2.0;
        double turnsToDestination = Math.sqrt(Cache.MY_LOCATION.distanceSquaredTo(location)) * averageCooldownTurns;
        double totalTurns = turnsToDestination + 10.0 * currentCooldown + 10.0 * destinationCooldown; // turns
        double unitsMissed = totalTurns / currentCooldown; // units
        double catchUpRate = 1.0 / destinationCooldown - 1.0 / currentCooldown; // number of more units per turn (units / turn)
        double payoffTurns = unitsMissed / catchUpRate;
        return 10.0 + 1.5 * payoffTurns < getNextVortexOrSingularity() - rc.getRoundNum();
    }

    public static int getNextVortexOrSingularity() {
        int currentRound = rc.getRoundNum();
        AnomalyScheduleEntry[] schedule = rc.getAnomalySchedule();
        int ret = 2000;
        for (int i = Math.min(20, schedule.length); --i >= 0;) {
            AnomalyScheduleEntry entry = schedule[i];
            if (entry.anomalyType == AnomalyType.VORTEX && entry.roundNumber >= currentRound) {
                ret = Math.min(ret, entry.roundNumber);
            }
        }
        return ret;
    }
}
