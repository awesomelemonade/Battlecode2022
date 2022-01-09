package fixedmicro.util;

public enum Profile {
    ERROR_STATE(true), CHUNK_INFO(false), PATHFINDER(false), EXPLORER(true), MINING(true), ATTACKING(true);

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
