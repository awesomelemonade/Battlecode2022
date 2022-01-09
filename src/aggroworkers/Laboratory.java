package aggroworkers;

import battlecode.common.GameActionException;
import aggroworkers.util.RunnableBot;

import static aggroworkers.util.Constants.ALLY_TEAM;
import static aggroworkers.util.Constants.rc;

public class Laboratory implements RunnableBot {
    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getTeamLeadAmount(ALLY_TEAM) >= 4000) {
            if (rc.canTransmute()) {
                rc.transmute();
            }
        }
    }
}
