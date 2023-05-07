package me.kous500.curvebuilding.fabric;

import java.util.Timer;
import java.util.TimerTask;

import static me.kous500.curvebuilding.fabric.PosDataPacket.SendPacketMembers;

public class SendPacketTimer {
    private static Timer timer;

    public static void start() {
        if (timer != null) stop();
        timer = new Timer();
        timer.schedule(new PlayerListUpdateTask(), 0, 1000);
    }

    public static void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class PlayerListUpdateTask extends TimerTask {
        @Override
        public void run() {
            SendPacketMembers();
        }
    }
}
