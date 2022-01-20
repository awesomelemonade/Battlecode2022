package earlierwatchtowers.util;

import battlecode.common.MapLocation;

import static earlierwatchtowers.util.Constants.rc;

public class ChunkAccessor {
    Node[][] nodes;
    LinkedList ll;

    public ChunkAccessor() {
        nodes = new Node[Communication.NUM_CHUNKS_WIDTH][Communication.NUM_CHUNKS_HEIGHT];
        ll = new LinkedList();
    }

    public void addChunk(int i, int j) {
        nodes[i][j] = ll.add(Communication.NUM_CHUNKS_HEIGHT * i + j);
    }

    public void removeChunk(int i, int j) {
        ll.remove(nodes[i][j]);
    }

    public int size() {
        return ll.size;
    }

    public MapLocation getRandom(int threshold) {
        if (ll.size <= 0) {
            return null;
        }
        Node cur = ll.front;
        for (int i = (int) (Math.random() * Math.min(threshold, ll.size)); --i >= 0;) {
            cur = cur.prev;
        }
        int chunkI = cur.val / Communication.NUM_CHUNKS_HEIGHT;
        int chunkJ = cur.val % Communication.NUM_CHUNKS_HEIGHT;
        int x = Communication.getChunkMidX(chunkI);
        int y = Communication.getChunkMidY(chunkJ);
        return new MapLocation(x, y);
    }

    public MapLocation getNearestChunk(int threshold) {
        MapLocation ourLoc = rc.getLocation();
        MapLocation bestLoc = null;
        int bestDist = (int)1e9;
        Node cur = ll.front;
        int cnt = Math.min(threshold, ll.size);
        for (int i = cnt; --i >= 0;) {
            int chunkI = cur.val / Communication.NUM_CHUNKS_HEIGHT;
            int chunkJ = cur.val % Communication.NUM_CHUNKS_HEIGHT;
            int x = Communication.getChunkMidX(chunkI);
            int y = Communication.getChunkMidY(chunkJ);
            MapLocation loc = new MapLocation(x, y);
            int dist = ourLoc.distanceSquaredTo(loc);
            if (dist < bestDist) {
                bestDist = dist;
                bestLoc = loc;
            }
            cur = cur.prev;
        }
        return bestLoc;
    }

    static class Node {
        public int val;
        public Node prev, next;

        public Node(int val) {
            this.val = val;
        }
    }

    static class LinkedList {
        public Node front;
        public int size;

        public LinkedList() {
            front = null;
            size = 0;
        }

        Node add(int val) {
            size++;
            Node node = new Node(val);
            if (front == null) {
                front = node;
            } else {
                node.prev = front;
                front.next = node;
                front = node;
            }
            return node;
        }

        void remove(Node node) {
            size--;
            if (node == front) {
                front = node.prev;
            }
            if (node.prev != null) node.prev.next = node.next;
            if (node.next != null) node.next.prev = node.prev;
        }
    }
}
