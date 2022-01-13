package opminermacro.util;

import battlecode.common.*;
import opminermacro.RobotPlayer;

import static opminermacro.util.Constants.rc;

public class Communication {
    private static final int CHUNK_INFO_OFFSET = 28; // 64 - 36 = 28
    public static final int CHUNK_SIZE = 5; // 5 x 5 chunks
    private static final int NUM_CHUNKS_SIZE = 12; // The entire map is 12 x 12 chunks
    private static final int BUFFER_SIZE = 36; // Number of 16 bit ints it would take in communication
    private static final int[] buffer = new int[BUFFER_SIZE];

    public static int NUM_CHUNKS_WIDTH;
    public static int NUM_CHUNKS_HEIGHT;

    public static final int CHUNK_INFO_UNEXPLORED = 0;
    public static final int CHUNK_INFO_ALLY = 1;
    public static final int CHUNK_INFO_ENEMY_GENERAL = 2;
    public static final int CHUNK_INFO_ENEMY_ARCHON = 3;
    public static final int CHUNK_INFO_LEAD = 1 << 2;

    private static final int ARCHON_LOCATIONS_OFFSET = 0;
    private static final int RESERVATION_OFFSET = 4;
    private static final int ARCHON_PORTABLE_OFFSET = 5;

    private static final int UNIT_COUNT_OFFSET = 6;
    private static final int UNIT_COUNT_MOD = 4096;
    private static final int NUM_UNIT_TYPES = 7;
    private static final int[] prevUnitCountValues = new int[NUM_UNIT_TYPES];
    private static final int[] currentUnitCount = new int[NUM_UNIT_TYPES];

    private static final int PASSIVE_SOLDIER_COUNT_OFFSET = 13;
    private static int prevPassiveSoldierCountValue = 0;
    private static int passiveSoldierCount = 0;

    public static void setPassiveSoldier() throws GameActionException {
        rc.writeSharedArray(PASSIVE_SOLDIER_COUNT_OFFSET, (rc.readSharedArray(PASSIVE_SOLDIER_COUNT_OFFSET) + 1) % UNIT_COUNT_MOD);
    }

    public static int getPassiveSoldierCount() {
        return passiveSoldierCount;
    }

    public static int getActiveSoldierCount() {
        return getAliveRobotTypeCount(RobotType.SOLDIER) - passiveSoldierCount;
    }

    public static int getAliveRobotTypeCount(RobotType type) {
        return currentUnitCount[type.ordinal()];
    }

    private static boolean chunksLoaded = false;
    private static boolean guessed = false;

    private static ChunkAccessor enemyGeneralChunkTracker;
    private static ChunkAccessor enemyArchonChunkTracker;
    private static ChunkAccessor safeLeadTracker;

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
        enemyGeneralChunkTracker = new ChunkAccessor();
        enemyArchonChunkTracker = new ChunkAccessor();
        safeLeadTracker = new ChunkAccessor();
    }

    private static final int ARCHON_PORTABLE_SET_BIT = 0;
    private static final int ARCHON_PORTABLE_HEARTBEAT_BIT = 1;
    private static int prevArchonPortableHeartbeatBit;

    public static void setPortableArchon() {
        try {
            if (rc.getRoundNum() != RobotPlayer.currentTurn) {
                // Went over bytecodes - Safety
                return;
            }
            // Check if already reserved
            int value = rc.readSharedArray(ARCHON_PORTABLE_OFFSET);
            if (((value >> ARCHON_PORTABLE_SET_BIT) & 0b1) == 0) {
                // No portable archon yet
                int heartbeat = (value >> ARCHON_PORTABLE_HEARTBEAT_BIT) & 0b1;
                int newValue = 0;
                newValue |= 0b1 << ARCHON_PORTABLE_SET_BIT;
                newValue |= ((1 - heartbeat) << ARCHON_PORTABLE_HEARTBEAT_BIT);
                rc.writeSharedArray(ARCHON_PORTABLE_OFFSET, newValue);
            }
        } catch (GameActionException ex) {
            ex.printStackTrace();
        }
    }

    public static boolean hasPortableArchon() {
        try {
            return ((rc.readSharedArray(ARCHON_PORTABLE_OFFSET) >> ARCHON_PORTABLE_SET_BIT) & 0b1) == 1;
        } catch (GameActionException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void clearStalePortableArchon() {
        if (rc.getRoundNum() != RobotPlayer.currentTurn) {
            // Went over bytecodes - Safety
            return;
        }
        if (Clock.getBytecodesLeft() < 300) {
            // Not enough bytecodes - Safety (at least 100 bytecodes needed to write to shared array)
            return;
        }
        if (Cache.TURN_COUNT == 1) {
            // Never clear on first turn - we never got a prevReservationHeartbeatBit yet
            return;
        }
        try {
            int value = rc.readSharedArray(ARCHON_PORTABLE_OFFSET);
            if (((value >> ARCHON_PORTABLE_SET_BIT) & 0b1) == 1) {
                int heartbeat = (value >> ARCHON_PORTABLE_HEARTBEAT_BIT) & 0b1;
                if (heartbeat == prevArchonPortableHeartbeatBit) { // if we see equivalent heartbeat
                    rc.writeSharedArray(ARCHON_PORTABLE_OFFSET, heartbeat << ARCHON_PORTABLE_HEARTBEAT_BIT); // clear (but keep heartbeat bit)
                }
            }
        } catch (GameActionException ex) {
            ex.printStackTrace();
        }
    }

    private static final int RESERVATION_SET_BIT = 0;
    private static final int RESERVATION_HEARTBEAT_BIT = 1;
    private static final int RESERVATION_GOLD_BIT = 2;
    private static final int RESERVATION_GOLD_MASK = 0b11;
    private static final int RESERVATION_LEAD_BIT = 3;
    private static final int RESERVATION_LEAD_MASK = 0b1111_1111_1111; // 13 bits
    private static final int RESERVATION_GOLD_INCREMENT = 50;
    private static int prevReservationHeartbeatBit;

    // Note: Can only reserve 50 or 100 gold and up to 2^12 = 4096 lead
    // Only reserves for 1 turn
    public static void reserve(int gold, int lead) {
        try {
            if (rc.getRoundNum() != RobotPlayer.currentTurn) {
                // Went over bytecodes - Safety
                return;
            }
            // Check if already reserved
            int value = rc.readSharedArray(RESERVATION_OFFSET);
            if (((value >> RESERVATION_SET_BIT) & 0b1) == 0) {
                // No reservation yet
                int heartbeat = (value >> RESERVATION_HEARTBEAT_BIT) & 0b1;
                int newValue = 0;
                newValue |= 0b1 << RESERVATION_SET_BIT;
                newValue |= ((1 - heartbeat) << RESERVATION_HEARTBEAT_BIT);
                newValue |= (gold / RESERVATION_GOLD_INCREMENT) << RESERVATION_GOLD_BIT;
                newValue |= lead << RESERVATION_LEAD_BIT;
                rc.writeSharedArray(RESERVATION_OFFSET, newValue);
            }
        } catch (GameActionException ex) {
            ex.printStackTrace();
        }
    }

    public static int getReservedGold() {
        try {
            int value = rc.readSharedArray(RESERVATION_OFFSET);
            return ((value >> RESERVATION_SET_BIT) & 0b1) * ((value >> RESERVATION_GOLD_BIT) & RESERVATION_GOLD_MASK) * RESERVATION_GOLD_INCREMENT;
        } catch (GameActionException e) {
            e.printStackTrace();
            return 1000;
        }
    }

    public static int getReservedLead() {
        try {
            int value = rc.readSharedArray(RESERVATION_OFFSET);
            return ((value >> RESERVATION_SET_BIT) & 0b1) * (value >> RESERVATION_LEAD_BIT) & RESERVATION_LEAD_MASK;
        } catch (GameActionException e) {
            e.printStackTrace();
            return 9999;
        }
    }

    public static void clearStaleReservation() {
        if (rc.getRoundNum() != RobotPlayer.currentTurn) {
            // Went over bytecodes - Safety
            return;
        }
        if (Clock.getBytecodesLeft() < 300) {
            // Not enough bytecodes - Safety (at least 100 bytecodes needed to write to shared array)
            return;
        }
        if (Cache.TURN_COUNT == 1) {
            // Never clear on first turn - we never got a prevReservationHeartbeatBit yet
            return;
        }
        try {
            int value = rc.readSharedArray(RESERVATION_OFFSET);
            if (((value >> RESERVATION_SET_BIT) & 0b1) == 1) {
                int heartbeat = (value >> RESERVATION_HEARTBEAT_BIT) & 0b1;
                if (heartbeat == prevReservationHeartbeatBit) { // if we see equivalent heartbeat
                    rc.writeSharedArray(RESERVATION_OFFSET, heartbeat << RESERVATION_HEARTBEAT_BIT); // clear (but keep heartbeat bit)
                }
            }
        } catch (GameActionException ex) {
            ex.printStackTrace();
        }
    }

    public static void loop() throws GameActionException {
        clearStaleReservation();
        clearStalePortableArchon();
        loadChunks();
        if (chunksLoaded && !guessed && Constants.ROBOT_TYPE == RobotType.ARCHON) {
            guessed = true;
            guessEnemyArchonLocations();
        }
        if (Cache.TURN_COUNT > 1) {
            for (int i = NUM_UNIT_TYPES; --i >= 0;) {
                int prev = rc.readSharedArray(UNIT_COUNT_OFFSET + i);
                currentUnitCount[i] = ((prev - prevUnitCountValues[i]) + UNIT_COUNT_MOD) % UNIT_COUNT_MOD;
            }
        }
        passiveSoldierCount = ((rc.readSharedArray(PASSIVE_SOLDIER_COUNT_OFFSET) - prevPassiveSoldierCountValue) + UNIT_COUNT_MOD) % UNIT_COUNT_MOD;
        prevPassiveSoldierCountValue = rc.readSharedArray(PASSIVE_SOLDIER_COUNT_OFFSET);
        for (int i = NUM_UNIT_TYPES; --i >= 0;) {
            prevUnitCountValues[i] = rc.readSharedArray(UNIT_COUNT_OFFSET + i);
        }
        int unitCountSharedIndex = UNIT_COUNT_OFFSET + Constants.ROBOT_TYPE.ordinal();
        rc.writeSharedArray(unitCountSharedIndex, (rc.readSharedArray(unitCountSharedIndex) + 1) % UNIT_COUNT_MOD);
    }

    public static void guessEnemyArchonLocations() {
        int symX = Constants.MAP_WIDTH - Cache.MY_LOCATION.x - 1;
        int symY = Constants.MAP_HEIGHT - Cache.MY_LOCATION.y - 1;
        setChunkTerritory(new MapLocation(Cache.MY_LOCATION.x, symY), CHUNK_INFO_ENEMY_GENERAL);
        setChunkTerritory(new MapLocation(symX, Cache.MY_LOCATION.y), CHUNK_INFO_ENEMY_GENERAL);
        setChunkTerritory(new MapLocation(symX, symY), CHUNK_INFO_ENEMY_GENERAL);
    }

    public static void loadChunks() throws GameActionException {
        int numChangesLeft = 4;
        for (int i = BUFFER_SIZE; --i >= 0;) {
            int sharedArrayIndex = CHUNK_INFO_OFFSET + i;
            int oldValue = buffer[i];
            int value = rc.readSharedArray(sharedArrayIndex);
            if (value != oldValue) {
                if (numChangesLeft == 0) {
                    // Save Bytecodes
                    chunksLoaded = false;
                    return;
                }
                numChangesLeft--;
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
                        int oldChunkLead = oldChunkValue & CHUNK_INFO_LEAD;
                        int chunkLead = chunkValue & CHUNK_INFO_LEAD;
                        if (oldChunkLead != chunkLead) {
                            onChunkLeadChange(oldChunkLead, chunkLead, chunkX, chunkY);
                        }
                        int oldChunkTerritory = oldChunkValue & 0b11;
                        int chunkTerritory = chunkValue & 0b11;
                        if (oldChunkTerritory != chunkTerritory) {
                            onChunkTerritoryChange(oldChunkTerritory, chunkTerritory, chunkX, chunkY);
                        }
                    }
                }
                buffer[i] = value;
            }
        }
        chunksLoaded = true;
    }

    public static void postLoop() throws GameActionException {
        prevReservationHeartbeatBit = (rc.readSharedArray(RESERVATION_OFFSET) >> RESERVATION_HEARTBEAT_BIT) & 0b1;
        prevArchonPortableHeartbeatBit = (rc.readSharedArray(ARCHON_PORTABLE_OFFSET) >> ARCHON_PORTABLE_HEARTBEAT_BIT) & 0b1;
        if (chunksLoaded) {
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
                    setChunkTerritory(currentChunkX, currentChunkY, CHUNK_INFO_ALLY);
                } else {
                    // Label as enemy
                    if (LambdaUtil.arraysAnyMatch(enemies, r -> r.type == RobotType.ARCHON)) {
                        // archon chunk
                        setChunkTerritory(currentChunkX, currentChunkY, CHUNK_INFO_ENEMY_ARCHON);
                    } else {
                        // general enemy chunk
                        int old = getChunkTerritory(currentChunkX, currentChunkY);
                        if (old != CHUNK_INFO_ENEMY_ARCHON) {
                            setChunkTerritory(currentChunkX, currentChunkY, CHUNK_INFO_ENEMY_GENERAL);
                        }
                    }
                }

                // Update chunk enemy is on
                RobotInfo closestEnemy = Util.getClosestEnemyRobot();
                if (closestEnemy != null) {
                    MapLocation loc = closestEnemy.location;
                    int chunkX = loc.x / CHUNK_SIZE;
                    int chunkY = loc.y / CHUNK_SIZE;
                    if (closestEnemy.type == RobotType.ARCHON) {
                        setChunkTerritory(chunkX, chunkY, CHUNK_INFO_ENEMY_ARCHON);
                    } else {
                        // general enemy chunk
                        int old = getChunkTerritory(chunkX, chunkY);
                        if (old != CHUNK_INFO_ENEMY_ARCHON) {
                            setChunkTerritory(chunkX, chunkY, CHUNK_INFO_ENEMY_GENERAL);
                        }
                    }
                }

                // Update current chunk lead
                MapLocation[] leadLocs = rc.senseNearbyLocationsWithLead();
                if (leadLocs.length > 0) {
                    setChunkLead(Util.random(leadLocs), true);
                }
            }

            // Flush Chunk Info Communication
            for (int i = BUFFER_SIZE; --i >= 0; ) {
                int sharedArrayIndex = CHUNK_INFO_OFFSET + i;
                if (rc.readSharedArray(sharedArrayIndex) != buffer[i]) {
                    rc.writeSharedArray(sharedArrayIndex, buffer[i]);
                }
            }
        }

        if (Profile.CHUNK_INFO.enabled()) {
            debug_drawChunks();
        }
    }

    public static void debug_drawChunks() {
        if (Constants.ROBOT_TYPE == RobotType.ARCHON || Constants.ROBOT_TYPE == RobotType.SOLDIER) {
            for (int i = NUM_CHUNKS_WIDTH; --i >= 0;) {
                for (int j = NUM_CHUNKS_HEIGHT; --j >= 0;) {
                    int midX = getChunkMidX(i);
                    int midY = getChunkMidY(j);
                    MapLocation location = new MapLocation(midX, midY);
                    int territory = getChunkTerritory(i, j);
                    switch (territory) {
                        case CHUNK_INFO_UNEXPLORED:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 255, 255, 255); // white
                            break;
                        case CHUNK_INFO_ALLY:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 0, 255, 0); // green
                            break;
                        case CHUNK_INFO_ENEMY_GENERAL:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 255, 200, 200); // pink
                            break;
                        case CHUNK_INFO_ENEMY_ARCHON:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 255, 0, 0); // red
                            break;
                        default:
                            Debug.setIndicatorDot(Profile.CHUNK_INFO, location, 0, 0, 0); // black
                            break;
                    }
                    boolean hasLead = chunkHasLead(i, j);
                    if (hasLead) {
                        Debug.setIndicatorDot(Profile.CHUNK_INFO, location.translate(0, -1), 50, 50, 50);
                    }
                }
            }
        }
    }

    public static void onChunkLeadChange(int oldValue, int value, int chunkX, int chunkY) {
        if (value == CHUNK_INFO_LEAD) {
            safeLeadTracker.addChunk(chunkX, chunkY);
        } else {
            safeLeadTracker.removeChunk(chunkX, chunkY);
        }
    }

    public static void onChunkTerritoryChange(int oldTerritory, int territory, int chunkX, int chunkY) {
        boolean fromEnemy = false;
        boolean toEnemy = false;
        switch (oldTerritory) {
            case CHUNK_INFO_ENEMY_ARCHON:
                enemyArchonChunkTracker.removeChunk(chunkX, chunkY);
            case CHUNK_INFO_ENEMY_GENERAL:
                enemyGeneralChunkTracker.removeChunk(chunkX, chunkY);
                fromEnemy = true;
        }
        switch (territory) {
            case CHUNK_INFO_ENEMY_ARCHON:
                enemyArchonChunkTracker.addChunk(chunkX, chunkY);
            case CHUNK_INFO_ENEMY_GENERAL:
                enemyGeneralChunkTracker.addChunk(chunkX, chunkY);
                toEnemy = true;
        }
        if (!fromEnemy && toEnemy) {
            if (chunkHasLead(chunkX, chunkY)) {
                safeLeadTracker.removeChunk(chunkX, chunkY);
            }
        } else if (fromEnemy && !toEnemy) {
            if (chunkHasLead(chunkX, chunkY)) {
                safeLeadTracker.addChunk(chunkX, chunkY);
            }
        }
    }

    public static MapLocation getClosestEnemyChunk() {
        return enemyGeneralChunkTracker.getNearestChunk(20);
    }

    public static MapLocation getClosestEnemyArchonChunk() {
        return enemyArchonChunkTracker.getNearestChunk(20);
    }

    public static int pack(MapLocation location) {
        return (location.x << 6) | location.y;
    }

    public static MapLocation unpack(int packed) {
        return new MapLocation((packed >> 6) & 0b111111, packed & 0b111111);
    }

    // SIGHT RADIUS = 20
    public static int getChunkMidX(int chunkX) {
        return Math.min(chunkX * CHUNK_SIZE + 2, Constants.MAP_WIDTH - 1);
    }

    public static int getChunkMidY(int chunkY) {
        return Math.min(chunkY * CHUNK_SIZE + 2, Constants.MAP_HEIGHT - 1);
    }

    public static void setChunkLead(int chunkX, int chunkY, boolean lead) {
        int chunkIndex = chunkX * NUM_CHUNKS_SIZE + chunkY; // [0-144]
        // CHUNKS_PER_SHARED_INTEGER = 4
        int sharedArrayIndex = chunkIndex >> 2; // divide by 4
        int sharedArrayOffset = chunkIndex & 0b11; // mod 4
        int value = buffer[sharedArrayIndex];
        int old;
        int leadBit = lead ? CHUNK_INFO_LEAD : 0;
        switch (sharedArrayOffset) {
            case 0:
                old = value & 0b0100;
                value = (value & 0b1111_1111_1111_1011) | leadBit;
                break;
            case 1:
                old = (value >> 4) & 0b0100;
                value = (value & 0b1111_1111_1011_1111) | (leadBit << 4);
                break;
            case 2:
                old = (value >> 8) & 0b0100;
                value = (value & 0b1111_1011_1111_1111) | (leadBit << 8);
                break;
            case 3:
                old = (value >> 12) & 0b0100;
                value = (value & 0b1011_1111_1111_1111) | (leadBit << 12);
                break;
            default:
                throw new IllegalArgumentException("Unknown");
        }
        buffer[sharedArrayIndex] = value;
        if (old != leadBit) {
            onChunkLeadChange(old, leadBit, chunkX, chunkY);
        }
    }

    public static void setChunkLead(MapLocation loc, boolean lead) {
        setChunkLead(loc.x/CHUNK_SIZE, loc.y/CHUNK_SIZE, lead);
    }

    public static void setChunkTerritory(int chunkX, int chunkY, int territory) {
        int chunkIndex = chunkX * NUM_CHUNKS_SIZE + chunkY; // [0-144]
        // CHUNKS_PER_SHARED_INTEGER = 4
        int sharedArrayIndex = chunkIndex >> 2; // divide by 4
        int sharedArrayOffset = chunkIndex & 0b11; // mod 4
        int value = buffer[sharedArrayIndex];
        int old;
        switch (sharedArrayOffset) {
            case 0:
                old = value & 0b0011;
                value = (value & 0b1111_1111_1111_1100) | territory;
                break;
            case 1:
                old = (value >> 4) & 0b0011;
                value = (value & 0b1111_1111_1100_1111) | (territory << 4);
                break;
            case 2:
                old = (value >> 8) & 0b0011;
                value = (value & 0b1111_1100_1111_1111) | (territory << 8);
                break;
            case 3:
                old = (value >> 12) & 0b0011;
                value = (value & 0b1100_1111_1111_1111) | (territory << 12);
                break;
            default:
                throw new IllegalArgumentException("Unknown");
        }
        buffer[sharedArrayIndex] = value;
        if (old != territory) {
            onChunkTerritoryChange(old, territory, chunkX, chunkY);
        }
    }

    public static void setChunkTerritory(MapLocation loc, int territory) {
        setChunkTerritory(loc.x/CHUNK_SIZE, loc.y/CHUNK_SIZE, territory);
    }

    private static boolean chunkHasLead(int chunkX, int chunkY) {
        int chunkIndex = chunkX * NUM_CHUNKS_SIZE + chunkY; // [0-144]
        // CHUNKS_PER_SHARED_INTEGER = 4
        int value = buffer[chunkIndex >> 2]; // sharedArrayIndex - divide by 4
        switch (chunkIndex & 0b11) { // sharedArrayOffset - mod 4
            case 0:
                return (value & 0b0000_0000_0000_0100) != 0;
            case 1:
                return (value & 0b0000_0000_0100_0000) != 0;
            case 2:
                return (value & 0b0000_0100_0000_0000) != 0;
            case 3:
                return (value & 0b0100_0000_0000_0000) != 0;
            default:
                throw new IllegalArgumentException("Unknown");
        }
    }

    public static int getChunkTerritory(int chunkX, int chunkY) {
        int chunkIndex = chunkX * NUM_CHUNKS_SIZE + chunkY; // [0-144]
        // CHUNKS_PER_SHARED_INTEGER = 4
        int value = buffer[chunkIndex >> 2]; // sharedArrayIndex - divide by 4
        switch (chunkIndex & 0b11) { // sharedArrayOffset - mod 4
            case 0:
                return value & 0b0011;
            case 1:
                return (value >> 4) & 0b0011;
            case 2:
                return (value >> 8) & 0b0011;
            case 3:
                return (value >> 12) & 0b0011;
            default:
                throw new IllegalArgumentException("Unknown");
        }
    }

    public static int getChunkTerritory(MapLocation loc) {
        return getChunkTerritory(loc.x / CHUNK_SIZE, loc.y / CHUNK_SIZE);
    }
}
