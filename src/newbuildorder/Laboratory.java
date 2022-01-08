package newbuildorder;

import battlecode.common.GameActionException;
import newbuildorder.util.RunnableBot;

import static newbuildorder.util.Constants.ALLY_TEAM;
import static newbuildorder.util.Constants.rc;

public class Laboratory implements RunnableBot {
    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        if (rc.getTeamLeadAmount(ALLY_TEAM) >= 5000) {
            if (rc.canTransmute()) {
                rc.transmute();
            }
        }
    }
}
