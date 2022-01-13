package opminermacro.util;

import static opminermacro.util.Constants.rc;

public enum Profile {
    ERROR_STATE(true),
    CHUNK_INFO(true),
    PATHFINDER(false),
    EXPLORER(false),
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
