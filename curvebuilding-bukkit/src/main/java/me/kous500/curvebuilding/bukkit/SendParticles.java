package me.kous500.curvebuilding.bukkit;

import com.github.fierioziy.particlenativeapi.api.particle.type.ParticleTypeMotion;
import com.github.fierioziy.particlenativeapi.api.utils.ParticleException;
import me.kous500.curvebuilding.math.PosData;
import me.kous500.curvebuilding.bukkit.config.BukkitConfig;
import me.kous500.curvebuilding.math.Vector3;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.UUID;

import static me.kous500.curvebuilding.bukkit.CurveBuildingPlugin.particles_1_13;
import static me.kous500.curvebuilding.Util.*;
import static java.lang.Math.sqrt;
import static me.kous500.curvebuilding.math.PosManager.getPosMap;
import static org.bukkit.Bukkit.getPlayer;

/**
 * Shows the curve positions of each player with particles. The positions are copied on the server thread, where the
 * commands change them, and the particles are sent from an asynchronous task.
 */
public class SendParticles {
    private static final long PERIOD_TICKS = 4;
    private static final int INTERVAL = 3;
    private static BukkitTask task;

    public static synchronized void start(Plugin plugin, BukkitConfig config) {
        stop();
        SendParticles sendParticles = new SendParticles(plugin, config);
        task = Bukkit.getScheduler().runTaskTimer(plugin, sendParticles::copyAndSend, 0, PERIOD_TICKS);
    }

    public static synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** The positions of a player and where the player is, copied on the server thread. */
    private record Target(Player player, World world, NavigableMap<Integer, Vector3[]> positions, Vector3 playerPosition) {}

    private final Plugin plugin;
    private final BukkitConfig config;
    private int r = 0;

    private SendParticles(Plugin plugin, BukkitConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    private void copyAndSend() {
        List<Target> targets = new ArrayList<>();
        for (Map.Entry<UUID, PosData> entry : getPosMap().entrySet()) {
            PosData posData = entry.getValue();
            Player player = getPlayer(entry.getKey());
            if (player == null || posData.getWorld() == null) continue;
            if (!posData.getWorld().getName().equals(player.getWorld().getName())) continue;

            NavigableMap<Integer, Vector3[]> positions = new TreeMap<>();
            posData.getMap().forEach((n, p) -> positions.put(n, p.clone()));
            if (positions.isEmpty()) continue;

            Location location = player.getLocation();
            targets.add(new Target(player, player.getWorld(), positions,
                    Vector3.at(location.getX(), location.getY(), location.getZ())));
        }

        int r = this.r;
        this.r = (r + 1) % INTERVAL;
        if (targets.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Target target : targets) {
                sendParticlePlayer(target, r);
            }
        });
    }

    private void sendParticlePlayer(Target target, int r) {
        NavigableMap<Integer, Vector3[]> positions = target.positions();
        int lastN = positions.lastKey();

        for (int n = 1; n <= lastN; n++) {
            Vector3[] p = positions.get(n);
            if (p != null) {
                for (int h = 0; h <= 2; h++) {
                    if (p[h] != null) {
                        Color color;
                        if (h == 1) color = config.fColor;
                        else if (h == 2) color = config.bColor;
                        else if (n == 1) color = config.startColor;
                        else if (n == lastN) color = config.endColor;
                        else color = config.posColor;

                        if (h == 0) sendCube(p[h], target, color);
                        else sendCross(p[h], target, color);
                    }
                }

                sendLine(p[0], p[1], target, particles_1_13.SOUL_FIRE_FLAME, r);
                sendLine(p[0], p[2], target, particles_1_13.SOUL_FIRE_FLAME, r);
            }
        }

        double curveLength = 0;
        for (Vector3[] segment : bezierSegments(positions)) {
            curveLength = sendBezier(segment, target, particles_1_13.FLAME, curveLength, r);
        }
    }

    private void sendCube(Vector3 pos, Target target, Color color) {
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
                    Location location = getLocation(target.world(), pos.add(ax, ay, az));

                    if ((isXEdge && isYEdge) || (isYEdge && isZEdge) || (isZEdge && isXEdge)) {
                        particles_1_13.DUST
                                .color(color, 1D)
                                .packet(true, location)
                                .sendTo(target.player());
                    }
                }
                Location location = getLocation(target.world(), pos.add(0.5, 0.5, 0.5));
                try {
                    particles_1_13.SOUL_FIRE_FLAME
                            .packet(true, location)
                            .sendTo(target.player());
                } catch (ParticleException e) {
                    particles_1_13.HAPPY_VILLAGER
                            .packet(true, location)
                            .sendTo(target.player());
                }
            }
        }
    }

    private void sendCross(Vector3 pos, Target target, Color color) {
        int density = (int) (config.posDensity * sqrt(3) / 2);

        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    for (int i = 0; i <= density; i++) {
                        double ax = 0.5 * x + (double) (density * x + (-x * 2 + 1) * i) / density * 0.5;
                        double ay = 0.5 * y + (double) (density * y + (-y * 2 + 1) * i) / density * 0.5;
                        double az = 0.5 * z + (double) (density * z + (-z * 2 + 1) * i) / density * 0.5;
                        Location location = getLocation(target.world(), pos.add(ax, ay, az));
                        particles_1_13.DUST
                                .color(color, 1D)
                                .packet(true, location)
                                .sendTo(target.player());
                    }
                }
                Location location = getLocation(target.world(), pos.add(0.5, 0.5, 0.5));
                try {
                    particles_1_13.SOUL_FIRE_FLAME
                            .packet(true, location)
                            .sendTo(target.player());
                } catch (ParticleException e) {
                    particles_1_13.HAPPY_VILLAGER
                            .packet(true, location)
                            .sendTo(target.player());
                }
            }
        }
    }

    private void sendLine(Vector3 pos1, Vector3 pos2, Target target, ParticleTypeMotion particleType, int r) {
        if (pos1 == null || pos2 == null) return;

        int distance = (int) pos1.distance(pos2);
        if (distance > config.lineMaxLength) return;

        int cnt = 0;
        Vector3 PlayerVec = target.playerPosition();
        Vector3 bVec = pos1;
        for (double i = 0; i <= 1; i += 1.0 / (distance * config.lineDensity)) {
            double x = (1 - i) * pos1.x() + i * pos2.x() + 0.5;
            double y = (1 - i) * pos1.y() + i * pos2.y() + 0.5;
            double z = (1 - i) * pos1.z() + i * pos2.z() + 0.5;
            Vector3 vec = Vector3.at(x, y, z);
            if ((PlayerVec.distance(vec) / 3000 * config.lineDensity) + (0.8 / config.lineDensity) < vec.distance(bVec)) {
                Location location = getLocation(target.world(), vec);
                if (cnt % INTERVAL == r) {
                    try {
                        particleType
                                .packet(true, location)
                                .sendTo(target.player());
                    } catch (ParticleException e) {
                        particles_1_13.HAPPY_VILLAGER
                                .packet(true, location)
                                .sendTo(target.player());
                    }
                }
                bVec = vec;
                cnt++;
            }
        }
    }

    private double sendBezier(Vector3[] p, Target target, ParticleTypeMotion particleType, double totalLength, int r) {
        if (totalLength > config.lineMaxLength || !isBezierSegmentWithin(p, config.lineMaxLength)) return config.lineMaxLength + 1;

        double length = totalLength + bezierLength(p, bezierFineness(p, 20));
        if (length > config.lineMaxLength) return length;

        int cnt = 0;
        Vector3 PlayerVec = target.playerPosition();
        Vector3 bVec = bezierCoordinate(p, 0);
        for (Vector3 vec : bezierPoints(p, Math.max(1, (int) Math.ceil(length * config.lineDensity)))) {
            if ((PlayerVec.distance(vec) / 3000 * config.lineDensity) + (0.8 / config.lineDensity) < vec.distance(bVec)) {
                Location location = getLocation(target.world(), vec);
                if (cnt % INTERVAL == r) {
                    try {
                        particleType
                                .packet(true, location)
                                .sendTo(target.player());
                    } catch (ParticleException e) {
                        particles_1_13.HAPPY_VILLAGER
                                .packet(true, location)
                                .sendTo(target.player());
                    }
                }
                bVec = vec;
                cnt++;
            }
        }

        return length;
    }

    private static Location getLocation(World world, Vector3 position) {
        return new Location(world, position.x(), position.y(), position.z());
    }
}
