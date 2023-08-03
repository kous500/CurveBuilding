package me.kous500.curvebuilding.bukkit;

import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleTypeMotion;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;
import me.kous500.curvebuilding.PosData;
import me.kous500.curvebuilding.bukkit.config.BukkitConfig;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static me.kous500.curvebuilding.bukkit.CurveBuildingPlugin.particles_1_13;
import static me.kous500.curvebuilding.PosData.getPosMap;
import static me.kous500.curvebuilding.Util.*;
import static com.sk89q.worldedit.bukkit.BukkitAdapter.adapt;
import static java.lang.Math.sqrt;
import static org.bukkit.Bukkit.getPlayer;

/**
 * posの状態を各プレイヤーに送信し続ける。
 */
public class SendParticles extends TimerTask {
    private static Timer timer;
    private static final int INTERVAL = 3;

    public static void start(BukkitConfig config) {
        if (timer != null) stop();
        timer = new Timer(true);
        timer.schedule(new SendParticles(config), 0, 200);
    }

    public static void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private final BukkitConfig config;
    private int r = 0;

    public SendParticles(BukkitConfig bukkitConfig) {
        this.config = bukkitConfig;
    }

    @Override
    public void run() {
        send();
    }

    private void send() {
        for (Map.Entry<UUID, PosData> entry : getPosMap().entrySet()) {
            PosData posData = entry.getValue();
            Player player = getPlayer(entry.getKey());

            sendParticlePlayer(posData, player);
        }

        r = (r + 1) % INTERVAL;
    }

    private void sendParticlePlayer(PosData posData, Player player) {
        if (player == null || posData.world == null || !posData.world.getName().equals(player.getWorld().getName())) {
            return;
        }

        boolean endLine = false;
        double curveLength = 0;

        for (int n = 1; n <= posData.p.lastEntry().getKey(); n++) {
            Vector3[] p = posData.p.get(n);
            if (p != null) {
                for (int h = 0; h <= 2; h++) {
                    if (p[h] != null) {
                        Color color;
                        if (h == 1) color = config.fColor;
                        else if (h == 2) color = config.bColor;
                        else if (n == 1) color = config.startColor;
                        else if (n == posData.p.lastEntry().getKey()) color = config.endColor;
                        else color = config.posColor;

                        if (h == 0) sendCube(p[h], posData.world, player, color);
                        else sendCross(p[h], posData.world, player, color);
                    }
                }

                sendLine(p[0], p[1], posData.world, player, particles_1_13.SOUL_FIRE_FLAME);
                sendLine(p[0], p[2], posData.world, player, particles_1_13.SOUL_FIRE_FLAME);

                if (p[0] == null) endLine = true;
            } else {
                endLine = true;
            }

            if (!endLine && n != 1) {
                Vector3[] bp = posData.p.get(n - 1);
                Vector3[] bezierPos = new Vector3[] {copyVector(bp[0]), copyVector(bp[2]), copyVector(p[1]), copyVector(p[0])};
                if (bezierPos[1] == null) bezierPos[1] = bezierPos[0];
                if (bezierPos[2] == null) bezierPos[2] = bezierPos[3];
                curveLength = sendBezier(bezierPos, posData.world, player, particles_1_13.FLAME, curveLength);
            }
        }
    }

    private void sendCube(Vector3 pos, World world, org.bukkit.entity.Player player, Color color) {
        int density = config.posDensity;

        for (int x = 0; x <= density; x++) {
            for (int y = 0; y <= density; y++) {
                for (int z = 0; z <= density; z++) {
                    boolean isXEdge = x == 0 || x == density;
                    boolean isYEdge = y == 0 || y == density;
                    boolean isZEdge = z == 0 || z == density;

                    double ax = x * (1.0 / density);
                    double ay = y * (1.0 / density);
                    double az = z * (1.0 / density);
                    Location location = adapt(adapt(world), pos.add(ax, ay, az));

                    if ((isXEdge && isYEdge) || (isYEdge && isZEdge) || (isZEdge && isXEdge)) {
                        particles_1_13.DUST
                                .color(color, 1D)
                                .packet(true, location)
                                .sendTo(player);
                    }
                }
                Location location = adapt(adapt(world), pos.add(0.5, 0.5, 0.5));
                try {
                    particles_1_13.SOUL_FIRE_FLAME
                            .packet(true, location)
                            .sendTo(player);
                } catch (ParticleException e) {
                    particles_1_13.HAPPY_VILLAGER
                            .packet(true, location)
                            .sendTo(player);
                }
            }
        }
    }

    private void sendCross(Vector3 pos, World world, org.bukkit.entity.Player player, Color color) {
        int density = (int) (config.posDensity * sqrt(3) / 2);

        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    for (int i = 0; i <= density; i++) {
                        double ax = 0.5 * x + (double) (density * x + (-x * 2 + 1) * i) / density * 0.5;
                        double ay = 0.5 * y + (double) (density * y + (-y * 2 + 1) * i) / density * 0.5;
                        double az = 0.5 * z + (double) (density * z + (-z * 2 + 1) * i) / density * 0.5;
                        Location location = adapt(adapt(world), pos.add(ax, ay, az));
                        particles_1_13.DUST
                                .color(color, 1D)
                                .packet(true, location)
                                .sendTo(player);
                    }
                }
                Location location = adapt(adapt(world), pos.add(0.5, 0.5, 0.5));
                try {
                    particles_1_13.SOUL_FIRE_FLAME
                            .packet(true, location)
                            .sendTo(player);
                } catch (ParticleException e) {
                    particles_1_13.HAPPY_VILLAGER
                            .packet(true, location)
                            .sendTo(player);
                }
            }
        }
    }

    private void sendLine(Vector3 pos1, Vector3 pos2, World world, org.bukkit.entity.Player player, ParticleTypeMotion particleType) {
        if (pos1 == null || pos2 == null) return;

        int distance = (int) pos1.distance(pos2);
        if (distance > config.lineMaxLength) return;

        int cnt = 0;
        Vector3 PlayerVec = adapt(player).getLocation().toVector();
        Vector3 bVec = pos1;
        for (double i = 0; i <= 1; i += 1.0 / (distance * config.lineDensity)) {
            double x = (1 - i) * pos1.getX() + i * pos2.getX() + 0.5;
            double y = (1 - i) * pos1.getY() + i * pos2.getY() + 0.5;
            double z = (1 - i) * pos1.getZ() + i * pos2.getZ() + 0.5;
            Vector3 vec = Vector3.at(x, y, z);
            if ((PlayerVec.distance(vec) / 3000 * config.lineDensity) + (0.8 / config.lineDensity) < vec.distance(bVec)) {
                Location location = adapt(adapt(world), vec);
                if (cnt % INTERVAL == r) {
                    try {
                        particleType
                                .packet(true, location)
                                .sendTo(player);
                    } catch (ParticleException e) {
                        particles_1_13.HAPPY_VILLAGER
                                .packet(true, location)
                                .sendTo(player);
                    }
                }
                bVec = vec;
                cnt++;
            }
        }
    }

    private double sendBezier(Vector3[] p, World world, org.bukkit.entity.Player player, ParticleTypeMotion particleType, double totalLength) {
        if (p[0] == null || p[1] == null || p[2] == null || p[3] == null) return config.lineMaxLength + 1;

        if (p[0].distance(p[3]) > config.lineMaxLength) return config.lineMaxLength + 1;
        if (p[0].distance(p[1]) > config.lineMaxLength) return config.lineMaxLength + 1;
        if (p[3].distance(p[2]) > config.lineMaxLength) return config.lineMaxLength + 1;

        double length = totalLength + bezierLength(p, p[0].distance(p[3]) * 20);
        if (length > config.lineMaxLength) return length;

        int cnt = 0;
        Vector3 PlayerVec = adapt(player).getLocation().toVector();
        Vector3 bVec = bezierCoordinate(p, 0);
        for (double i = 0; i <= 1; i += 1.0 / (length * config.lineDensity)) {
            Vector3 vec = bezierCoordinate(p, i);
            if ((PlayerVec.distance(vec) / 3000 * config.lineDensity) + (0.8 / config.lineDensity) < vec.distance(bVec)) {
                Location location = BukkitAdapter.adapt(adapt(world), vec);
                if (cnt % INTERVAL == r) {
                    try {
                        particleType
                                .packet(true, location)
                                .sendTo(player);
                    } catch (ParticleException e) {
                        particles_1_13.HAPPY_VILLAGER
                                .packet(true, location)
                                .sendTo(player);
                    }
                }
                bVec = vec;
                cnt++;
            }
        }

        return length;
    }
}