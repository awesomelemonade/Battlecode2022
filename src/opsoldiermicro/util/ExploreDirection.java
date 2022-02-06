package opsoldiermicro.util;

import battlecode.common.Direction;

public enum ExploreDirection {
    N(0, 1),
    S(0, -1),
    E(1, 0),
    W(-1, 0),
    NW(-1, 1),
    NE(1, 1),
    SE(1, -1),
    SW(-1, -1),
    NNW(-1, 2),
    NWW(-2, 1),
    SWW(-2, -1),
    SSW(-1, -2),
    SSE(1, -2),
    SEE(2, -1),
    NEE(2, 1),
    NNE(1, 2);

    public final int dx;
    public final int dy;

    private ExploreDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static boolean isOpposite(ExploreDirection a, ExploreDirection b) {
        return a.dx == -b.dx && a.dy == -b.dy;
    }

    public static ExploreDirection fromDirection(Direction direction) {
        switch (direction) {
            case NORTH:
                return N;
            case SOUTH:
                return S;
            case EAST:
                return E;
            case WEST:
                return W;
            case NORTHEAST:
                return NE;
            case NORTHWEST:
                return NW;
            case SOUTHEAST:
                return SE;
            case SOUTHWEST:
                return SW;
            default:
                throw new IllegalArgumentException("Unknown");
        }
    }
}
