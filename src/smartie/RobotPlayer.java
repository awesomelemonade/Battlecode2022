package smartie;

import battlecode.common.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static final Random rng = new Random(6147);

    /**
     * Array containing all the possible movement directions.
     */
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static final int[][] movements = {{0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}};
    static boolean foundPermSpot = false;
    static boolean currentlyFixing = false;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc The RobotController object. You use it to perform actions from this robot, and to get
     *           information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());

        // You can also use indicators to save debug notes in replays.
        rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!
            System.out.println("Age: " + turnCount + "; Location: " + rc.getLocation());

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case ARCHON:
                        runArchon(rc);
                        break;
                    case MINER:
                        runMiner(rc);
                        break;
                    case SOLDIER:
                        runSoldier(rc);
                        break;
                    case LABORATORY: // Examplefuncsplayer doesn't use any of these robot types below.
                    case WATCHTOWER: // You might want to give them a try!
                        runWatchtower(rc);
                        break;
                    case BUILDER:
                        runBuilder(rc);
                        break;
                    case SAGE:
                        break;
                }
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        if (rc.getRoundNum() == 1) {
            rc.writeSharedArray(0, 0);
//            int archonCount = rc.getArchonCount();
//            MapLocation currentLocation = rc.getLocation();
//            rc.writeSharedArray(archonCount * 2, currentLocation.x);
//            rc.writeSharedArray(archonCount - 1 * 2 + 1, currentLocation.y);
//            rc.writeSharedArray(0, rc.getLocation().x - 4);
//            rc.writeSharedArray(1, rc.getLocation().y + 4);
//            rc.setIndicatorString("Archan Location: (" + currentLocation.x + ", " + currentLocation.y + ")");
        }

        Direction dir = directions[rng.nextInt(directions.length)];


        if (rc.getRoundNum() < 50) {
            //rc.setIndicatorString("Trying to build initial miners");
            for (int i = 0; i < directions.length; i++) {
                if (rc.canBuildRobot(RobotType.MINER, directions[i])) {
                    rc.buildRobot(RobotType.MINER, directions[i]);
                    break;
                }
            }
        } else {
//            if (rc.getTeamLeadAmount(rc.getTeam()) < 100) {
//                // Let's try to build a miner.
//                rc.setIndicatorString("Trying to build a miner");
//                for (int i = 0; i < directions.length; i++) {
//                    if (rc.canBuildRobot(RobotType.MINER, directions[i])) {
//                        rc.buildRobot(RobotType.MINER, directions[i]);
//                        break;
//                    }
//                }
//            } else {
                // Let's try to build a soldier.
                double prob = Math.random();
                if (rc.readSharedArray(0) == 0) {
                    for (int i = 0; i < directions.length; i++) {
                        if (rc.canBuildRobot(RobotType.SOLDIER, directions[i])) {
                            rc.buildRobot(RobotType.SOLDIER, directions[i]);
                            break;
                        }
                    }
                }
                if (rc.getTeamLeadAmount(rc.getTeam()) > 75) {
                    if (prob >= 0.35) {
                        rc.setIndicatorString("Trying to build a soldier");
                        for (int i = 0; i < directions.length; i++) {
                            if (rc.canBuildRobot(RobotType.SOLDIER, directions[i])) {
                                rc.buildRobot(RobotType.SOLDIER, directions[i]);
                                break;
                            }
                        }
                    }
                    else {
                        rc.setIndicatorString("Trying to build a builder");
                        for (int i = 0; i < directions.length; i++) {
                            if (rc.canBuildRobot(RobotType.BUILDER, directions[i])) {
                                rc.buildRobot(RobotType.BUILDER, directions[i]);
                                break;
                            }
                        }
                    }
                }
            }
        }
//    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation) && rc.senseLead(mineLocation) > 1) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Also try to move randomly.
        if (!foundPermSpot) {
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
                MapLocation currentLocation = rc.getLocation();
                if (currentLocation.x % 3 == 1 && currentLocation.y % 2 == 0) {
                    foundPermSpot = true;
                }
                System.out.println("I moved!");
            }
        }
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length == 0 && rc.canSenseLocation(new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2)))) {
            rc.writeSharedArray(0, 0);
        }
        if (enemies.length > 0) {
            Arrays.sort(enemies, Comparator.comparingInt(x -> x.getHealth()));
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
            rc.writeSharedArray(0, 1);
            rc.writeSharedArray(1, toAttack.x);
            rc.writeSharedArray(2, toAttack.y);
        }

        // Also try to move randomly.
        rc.setIndicatorString("Attack Token: " + rc.readSharedArray(0) + " Location = " + rc.readSharedArray(1) + " " + rc.readSharedArray(2));
        int funnel = rc.readSharedArray(0);
        if (funnel == 0) {
            Direction dir = directions[rng.nextInt(directions.length)];
            if (rc.canMove(dir)) {
                rc.move(dir);
                System.out.println("I moved!");
            }
        }
        else {
            MapLocation funnelTowards = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
            MapLocation currentLocation = rc.getLocation();
            int nextMove = -1;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < directions.length; i++) {
                if (!rc.canMove(directions[i])) {
                    continue;
                }
                MapLocation nextLocation = new MapLocation(currentLocation.x + movements[i][0], currentLocation.y + movements[i][1]);
                int euclidDistance = Math.abs(funnelTowards.x - nextLocation.x) + Math.abs(funnelTowards.y - nextLocation.y);
                if (minDistance > euclidDistance) {
                    minDistance = euclidDistance;
                    nextMove = i;
                }
            }
            rc.move(directions[nextMove]);
        }
    }

    static void runWatchtower(RobotController rc) throws GameActionException {
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(20, opponent);
        if (enemies.length > 0) {
            Arrays.sort(enemies, Comparator.comparingInt(x -> x.getHealth()));
            MapLocation toAttack = enemies[0].location;
            rc.setIndicatorString("Targeting: " + toAttack.x + " " + toAttack.y + " " + rc.canAttack(toAttack));
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

    }

    static void runBuilder(RobotController rc) throws GameActionException {
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(5);
        for (int i = 0; i <nearbyRobots.length; i++) {
            if (nearbyRobots[i].type.equals(RobotType.WATCHTOWER) && nearbyRobots[i].getHealth() != 130) {
                if (rc.canRepair(nearbyRobots[i].location)) {
                    rc.repair(nearbyRobots[i].location);
                }
                currentlyFixing = true;
            }
            if (nearbyRobots[i].type.equals(RobotType.WATCHTOWER) && nearbyRobots[i].getHealth() == 130) {
                currentlyFixing = false;
            }
        }
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (!currentlyFixing && enemies.length == 0 && rc.canSenseLocation(new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2)))) {
            rc.writeSharedArray(0, 0);
        }
        if (!currentlyFixing) {
            MapLocation funnelTowards = new MapLocation(rc.readSharedArray(1), rc.readSharedArray(2));
            MapLocation currentLocation = rc.getLocation();
            int nextMove = -1;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 0; i < directions.length; i++) {
                if (!rc.canMove(directions[i])) {
                    continue;
                }
                MapLocation nextLocation = new MapLocation(currentLocation.x + movements[i][0], currentLocation.y + movements[i][1]);
                int euclidDistance = Math.abs(funnelTowards.x - nextLocation.x) + Math.abs(funnelTowards.y - nextLocation.y);
                if (minDistance > euclidDistance) {
                    minDistance = euclidDistance;
                    nextMove = i;
                }
            }
            rc.move(directions[nextMove]);
            int euclidDistance = Math.abs(funnelTowards.x - currentLocation.x) + Math.abs(funnelTowards.y - currentLocation.y);
            if (euclidDistance <= 12) {
                for (int i = 0; i < directions.length; i++) {
                    if (rc.canBuildRobot(RobotType.WATCHTOWER, directions[i]) && (rc.getLocation().add(directions[i]).x + rc.getLocation().add(directions[i]).y) % 2 == 0 && rc.getLocation().add(directions[i]).x % 2 == 0) {
                        rc.buildRobot(RobotType.WATCHTOWER, directions[i]);
                    }
                }

            }
        }
    }
}
