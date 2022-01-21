package sages;

import battlecode.common.GameActionException;
import sages.util.RunnableBot;

import static sages.util.Constants.ALLY_TEAM;
import static sages.util.Constants.rc;

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
