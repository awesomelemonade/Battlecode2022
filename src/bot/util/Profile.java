package bot.util;

public enum Profile {
    CHUNK_INFO(true), PATHFINDER(true);

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
