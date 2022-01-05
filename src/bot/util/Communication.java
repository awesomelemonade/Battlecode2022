package bot.util;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

import static bot.util.Constants.rc;

public class Communication {
    private static final int CHUNK_INFO_OFFSET = 28; // 64 - 36 = 28
    private static final int CHUNK_SIZE = 5; // 5 x 5 chunks
    private static final int NUM_CHUNKS_SIZE = 12; // The entire map is 12 x 12 chunks
    private static final int BUFFER_SIZE = 36; // Number of 16 bit ints it would take in communication
    private static final int[] buffer = new int[BUFFER_SIZE];

    private static int NUM_CHUNKS_WIDTH;
    private static int NUM_CHUNKS_HEIGHT;

    public static final int CHUNK_INFO_UNEXPLORED = 0;
    public static final int CHUNK_INFO_ALLY = 1;
    public static final int CHUNK_INFO_ENEMY = 2;

    private static final int ARCHON_LOCATIONS_OFFSET = 0;

    public static void init() throws GameActionException {
        NUM_CHUNKS_WIDTH = (Constants.MAP_WIDTH + (CHUNK_SIZE - 1)) / CHUNK_SIZE;
        NUM_CHUNKS_HEIGHT = (Constants.MAP_HEIGHT + (CHUNK_SIZE - 1)) / CHUNK_SIZE;
        // Set Archon location
        if (Constants.ROBOT_TYPE == RobotType.ARCHON) {
            // Find next available slot
            for (int i = 0; i < Constants.MAX_ARCHONS; i++) {
                int sharedArrayIndex = ARCHON_LOCATIONS_OFFSET + i;
                if (rc.readSharedArray(sharedArrayIndex) == 0) {
                    // Write
                    rc.writeSharedArray(sharedArrayIndex, (pack(rc.getLocation()) << 1) | 1);
                    break;
                }
            }
        } else {
            // Read archon locations
            boolean initialized = false;
            for (int i = Constants.MAX_ARCHONS; --i >= 0;) {
                int value = rc.readSharedArray(i);
                if (value != 0) {
                    if (!initialized) {
                        MapInfo.INITIAL_ARCHON_LOCATIONS = new MapLocation[i + 1];
                        initialized = true;
                    }
                    MapInfo.INITIAL_ARCHON_LOCATIONS[i] = unpack(value >> 1);
                }
            }
            if (!initialized) {
                throw new IllegalStateException("Cannot read any archon locations");
            }
        }
    }

    public static void loop() throws GameActionException {
        for (int i = BUFFER_SIZE; --i >= 0;) {
            int sharedArrayIndex = CHUNK_INFO_OFFSET + i;
            int oldValue = buffer[i];
            int value = rc.readSharedArray(sharedArrayIndex);
            if (value != oldValue) {
                for (int j = 4; --j >= 0;) {
                    int oldChunkValue;
                    int chunkValue;
                    switch (j) {
                        case 0:
                            oldChunkValue = oldValue & 0b1111;
                            chunkValue = value & 0b1111;
                            break;
                        case 1:
                            oldChunkValue = (oldValue >> 4) & 0b1111;
                            chunkValue = (value >> 4) & 0b1111;
                            break;
                        case 2:
                            oldChunkValue = (oldValue >> 8) & 0b1111;
                            chunkValue = (value >> 8) & 0b1111;
                            break;
                        case 3:
                            oldChunkValue = (oldValue >> 12) & 0b1111;
                            chunkValue = (value >> 12) & 0b1111;
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                    if (chunkValue != oldChunkValue) {
                        int chunkIndex = (i << 2) + j;
                        int chunkX = chunkIndex / NUM_CHUNKS_SIZE;
                        int chunkY = chunkIndex % NUM_CHUNKS_SIZE;
                        int chunkMidX = getChunkMidX(chunkX);
                        int chunkMidY = getChunkMidY(chunkY);
                        MapLocation chunkLocation = new MapLocation(chunkMidX, chunkMidY);
                        if (oldChunkValue == CHUNK_INFO_ENEMY) {
                            // Remove from enemy list
                            Debug.setIndicatorDot(chunkLocation, 0, 255, 255);
                        }
                        if (chunkValue == CHUNK_INFO_ENEMY) {
                            // Add to enemy list
                            Debug.setIndicatorDot(chunkLocation, 0, 0, 255);
                        }
                    }
                }
                buffer[i] = value;
            }
        }
    }

    public static void postLoop() throws GameActionException {
        // Mark areas that are friendly
        MapLocation currentLocation = rc.getLocation();
        int currentChunkX = currentLocation.x / CHUNK_SIZE;
        int currentChunkY = currentLocation.y / CHUNK_SIZE;
        MapLocation chunkMid = new MapLocation(getChunkMidX(currentChunkX), getChunkMidY(currentChunkY));
        if (currentLocation.isAdjacentTo(chunkMid)) { // hasEntireChunkInVision
            // Label Chunk
            // RobotInfo[] friendlies = rc.senseNearbyRobots(chunkMid, 8, Constants.ALLY_TEAM);
            RobotInfo[] enemies = rc.senseNearbyRobots(chunkMid, 8, Constants.ENEMY_TEAM); // 8 = dist squared for 5 x 5
            if (enemies.length == 0) {
                // Label as ally
                setChunkInfo(currentChunkX, currentChunkY, CHUNK_INFO_ALLY);
            } else {
                // Label as enemy
                setChunkInfo(currentChunkX, currentChunkY, CHUNK_INFO_ENEMY);
            }
        }

        // Flush Chunk Info Communication
        for (int i = BUFFER_SIZE; --i >= 0;) {
            int sharedArrayIndex = CHUNK_INFO_OFFSET + i;
            if (rc.readSharedArray(sharedArrayIndex) != buffer[i]) {
                rc.writeSharedArray(sharedArrayIndex, buffer[i]);
            }
        }

        // Debug Draw
        if (Constants.ROBOT_TYPE == RobotType.ARCHON) {
            for (int i = NUM_CHUNKS_WIDTH; --i >= 0;) {
                for (int j = NUM_CHUNKS_HEIGHT; --j >= 0;) {
                    int midX = getChunkMidX(i);
                    int midY = getChunkMidY(j);
                    MapLocation location = new MapLocation(midX, midY);
                    int info = getChunkInfo(i, j);
                    switch (info) {
                        case CHUNK_INFO_UNEXPLORED:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 255, 255, 255); // white
                            break;
                        case CHUNK_INFO_ALLY:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 0, 255, 0); // green
                            break;
                        case CHUNK_INFO_ENEMY:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 255, 0, 0); // red
                            break;
                        default:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 0, 0, 0); // black
                            break;
                    }
                }
            }
        }
    }

    public static int pack(MapLocation location) {
        return (location.x << 6) | location.y;
    }

    public static MapLocation unpack(int packed) {
        return new MapLocation((packed >> 6) & 0b111111, packed & 0b111111);
    }

    // SIGHT RADIUS = 20
    public static int getChunkMidX(int chunkX) {
        return Math.min(chunkX * CHUNK_SIZE + 2, Constants.MAP_WIDTH - 3);
    }

    public static int getChunkMidY(int chunkY) {
        return Math.min(chunkY * CHUNK_SIZE + 2, Constants.MAP_HEIGHT - 3);
    }

    public static void setChunkInfo(int chunkX, int chunkY, int info) {
        int chunkIndex = chunkX * NUM_CHUNKS_SIZE + chunkY; // [0-144]
        // CHUNKS_PER_SHARED_INTEGER = 4
        int sharedArrayIndex = chunkIndex >> 2; // divide by 4
        int sharedArrayOffset = chunkIndex & 0b11; // mod 4
        int value = buffer[sharedArrayIndex];
        switch (sharedArrayOffset) {
            case 0:
                value = (value & 0b1111_1111_1111_0000) | info;
                break;
            case 1:
                value = (value & 0b1111_1111_0000_1111) | (info << 4);
                break;
            case 2:
                value = (value & 0b1111_0000_1111_1111) | (info << 8);
                break;
            case 3:
                value = (value & 0b0000_1111_1111_1111) | (info << 12);
                break;
            default:
                throw new IllegalArgumentException("Unknown");
        }
        buffer[sharedArrayIndex] = value;
    }

    public static int getChunkInfo(int chunkX, int chunkY) {
        int chunkIndex = chunkX * NUM_CHUNKS_SIZE + chunkY; // [0-144]
        // CHUNKS_PER_SHARED_INTEGER = 4
        int sharedArrayIndex = chunkIndex >> 2; // divide by 4
        int sharedArrayOffset = chunkIndex & 0b11; // mod 4
        int value = buffer[sharedArrayIndex];
        switch (sharedArrayOffset) {
            case 0:
                return value & 0b1111;
            case 1:
                return (value >> 4) & 0b1111;
            case 2:
                return (value >> 8) & 0b1111;
            case 3:
                return (value >> 12) & 0b1111;
            default:
                throw new IllegalArgumentException("Unknown");
        }
    }

    public static int getChunkInfo(MapLocation loc) {
        return getChunkInfo(loc.x / CHUNK_SIZE, loc.y / CHUNK_SIZE);
    }
}
