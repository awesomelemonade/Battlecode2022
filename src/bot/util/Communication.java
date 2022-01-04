package bot.util;

import battlecode.common.GameActionException;

import static bot.util.Constants.rc;

public class Communication {
    private static final int MAP_INFO_OFFSET = 0;
    private static final int CHUNK_SIZE = 5; // 5 x 5 chunks
    private static final int NUM_CHUNKS_SIZE = 12; // The entire map is 12 x 12 chunks
    private static final int BUFFER_SIZE = 36; // Number of 16 bit ints it would take in communication
    private static final int[] buffer = new int[BUFFER_SIZE];

    public static void loop() throws GameActionException {
        for (int i = BUFFER_SIZE; --i >= 0;) {
            int sharedArrayIndex = MAP_INFO_OFFSET + i;
            buffer[i] = rc.readSharedArray(sharedArrayIndex); // TODO: Detect Change?
        }
    }

    public static void postLoop() throws GameActionException {
        for (int i = BUFFER_SIZE; --i >= 0;) {
            int sharedArrayIndex = MAP_INFO_OFFSET + i;
            if (rc.readSharedArray(sharedArrayIndex) != buffer[i]) {
                rc.writeSharedArray(sharedArrayIndex, buffer[i]);
            }
        }
    }
    public static int toChunkCoord(int coord) {
        return coord / CHUNK_SIZE;
    }

    public static void setMapInfo(int chunkX, int chunkY, int info) {
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

    public static int getMapInfo(int chunkX, int chunkY) {
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
}
