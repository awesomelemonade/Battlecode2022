package bot.util;

public enum Profile {
    ERROR_STATE(true), CHUNK_INFO(true), PATHFINDER(true), EXPLORER(true);

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
