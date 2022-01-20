package opminermicro3;

import battlecode.common.GameActionException;
import opminermicro3.util.RunnableBot;

import static opminermicro3.util.Constants.ALLY_TEAM;
import static opminermicro3.util.Constants.rc;

public class Laboratory implements RunnableBot {
    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        int lead = rc.getTeamLeadAmount(ALLY_TEAM);
        if (lead >= 4000 || (rc.getRoundNum() > 1900 && lead >= 150) || rc.getRoundNum() > 1950) {
            if (rc.canTransmute()) {
                rc.transmute();
            }
        }
    }
}
