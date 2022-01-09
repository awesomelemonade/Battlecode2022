package fixedmicro;

import battlecode.common.GameActionException;
import fixedmicro.util.RunnableBot;

import static fixedmicro.util.Constants.ALLY_TEAM;
import static fixedmicro.util.Constants.rc;

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
