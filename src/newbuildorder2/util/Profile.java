package newbuildorder2.util;

import static newbuildorder2.util.Constants.rc;

public enum Profile {
    ERROR_STATE(true),
    CHUNK_INFO(false),
    PATHFINDER(false),
    EXPLORER(false),
    MINING(false),
    ATTACKING(false);

    private static final int id = -1; // Filter by id

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled && (id == -1 || id == rc.getID());
    }
}
