package newbuildorder2.util;

import static newbuildorder2.util.Constants.rc;

public enum Profile {
    ERROR_STATE(true),
    CHUNK_INFO(false),
    PATHFINDER(false),
    EXPLORER(true),
    MINING(false),
    ATTACKING(false);

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
