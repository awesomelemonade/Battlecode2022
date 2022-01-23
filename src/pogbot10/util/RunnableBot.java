package pogbot10.util;

import battlecode.common.GameActionException;

public interface RunnableBot {
    public void init() throws GameActionException;
    public void loop() throws GameActionException;
}
