package newbuildorder4.util;

import static newbuildorder4.util.Constants.rc;

public enum Profile {
    ERROR_STATE(true),
    CHUNK_INFO(false),
    PATHFINDER(true),
    EXPLORER(true),
    MINING(true),
    ATTACKING(false);

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
