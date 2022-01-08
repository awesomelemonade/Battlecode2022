package newbuildorder;

import battlecode.common.*;
import newbuildorder.util.*;

import static newbuildorder.util.Constants.*;

public class Archon implements RunnableBot {
    private int minerCount;

    @Override
    public void init() throws GameActionException {
        minerCount = 0;
    }

    @Override
    public void loop() throws GameActionException {
        // TODO: build more miners based on lead communication
        if (tryBuildDefenders()) return;
        if (tryBuildRich()) return;
        if (tryBuildPoor()) return;
        tryRepair();
    }

    public boolean tryBuildAttacker() throws GameActionException {
        if (rc.getTeamGoldAmount(ALLY_TEAM) >= RobotType.SAGE.buildCostGold) {
            return tryBuild(RobotType.SAGE);
        }
        return tryBuild(RobotType.SOLDIER);
    }

    public boolean tryBuildDefenders() throws GameActionException {
        RobotInfo closestEnemy = Util.getClosestEnemyRobot();
        if (closestEnemy != null && closestEnemy.location.isWithinDistanceSquared(rc.getLocation(), RobotType.ARCHON.visionRadiusSquared)) {
            tryBuildAttacker();
            return true;
        }
        return false;
    }

    public boolean tryBuildRich() throws GameActionException {
        if (rc.getTeamLeadAmount(ALLY_TEAM) >= 500) {
            double r = Math.random();
            if (r < 0.05) {
                tryBuild(RobotType.MINER);
            } else if (Math.random() < 0.15) {
                tryBuild(RobotType.BUILDER);
            } else {
                tryBuildAttacker();
            }
            return true;
        }
        return false;
    }

    public boolean tryBuildPoor() throws GameActionException {
        int mapSize = rc.getMapWidth() + rc.getMapHeight();
        if (minerCount <= 5 + mapSize/10) {
            tryBuild(RobotType.MINER);
        } else {
            double r = Math.random();
            if (r < 0.2) {
                tryBuild(RobotType.MINER);
            } else {
                tryBuildAttacker();
            }
        }
        return true;
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
                        if (type == RobotType.MINER) {
                            minerCount++;
                        }
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
                    if (type == RobotType.MINER) {
                        minerCount++;
                    }
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
            if (ret == null) {
                return Util.randomAdjacentDirection();
            } else {
                return ret;
            }
        }
    }
}
