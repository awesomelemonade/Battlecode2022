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
        if (rc.canTransmute()) {
            rc.transmute();
        }
    }
}
