package passiveRetreat;

import battlecode.common.GameActionException;
import passiveRetreat.util.RunnableBot;

import static passiveRetreat.util.Constants.ALLY_TEAM;
import static passiveRetreat.util.Constants.rc;

public class Laboratory implements RunnableBot {
    @Override
    public void init() throws GameActionException {

    }

    @Override
    public void loop() throws GameActionException {
        int lead = rc.getTeamLeadAmount(ALLY_TEAM);
        if (lead >= 4000 || (rc.getRoundNum() > 1900 && lead > 300) || rc.getRoundNum() > 1950) {
            if (rc.canTransmute()) {
                rc.transmute();
            }
        }
    }
}
