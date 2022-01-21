package opsoldiermicro2.util;

public enum Profile {
    ERROR_STATE(true),
    CHUNK_INFO(false),
    PATHFINDER(false),
    EXPLORER(false),
    MINING(false),
    ATTACKING(true);

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
