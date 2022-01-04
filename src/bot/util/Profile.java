package bot.util;

public enum Profile {
    MAIN(true), PATHFINDER(true);

    private final boolean enabled;

    private Profile(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }
}
