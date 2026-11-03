package me.kous500.curvebuilding.bukkit;

import com.github.fierioziy.particlenativeapi.api.packet.ParticlePacket;
import com.github.fierioziy.particlenativeapi.api.particle.ParticleList_1_13;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import me.kous500.curvebuilding.bukkit.config.BukkitConfig;
import me.kous500.curvebuilding.curve.CurvePoint;
import me.kous500.curvebuilding.curve.PosData;
import me.kous500.curvebuilding.math.Vector3;
import me.kous500.curvebuilding.preview.ParticleLayout;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import static me.kous500.curvebuilding.bukkit.CurveBuildingPlugin.particles_1_13;
import static me.kous500.curvebuilding.curve.PosManager.getPosMap;
import static org.bukkit.Bukkit.getPlayer;

/**
 * Shows the curve positions of each player with particles. The players and their positions are taken on the server
 * thread, and the particles are sent from an asynchronous task.
 */
public class SendParticles {
    private static final long PERIOD_TICKS = 4;
    private static @Nullable BukkitTask task;

    /**
     * Starts showing the curves, after stopping what was started before. Nothing is started when ParticleNativeAPI
     * could not be loaded.
     *
     * @param plugin the plugin to run the tasks for
     * @param config the config for the particles
     */
    public static synchronized void start(Plugin plugin, BukkitConfig config) {
        stop();
        ParticleList_1_13 particleList = particles_1_13;
        if (particleList == null) return;
        SendParticles sendParticles = new SendParticles(plugin, config, particleList);
        task = Bukkit.getScheduler().runTaskTimer(plugin, sendParticles::copyAndSend, 0, PERIOD_TICKS);
    }

    /**
     * Stops showing the curves.
     */
    public static synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** The positions of a player and where the player is, taken on the server thread. */
    private record Target(
            Player player,
            World world,
            NavigableMap<Integer, CurvePoint> positions,
            Vector3 playerPosition) {}

    private final Plugin plugin;
    private final BukkitConfig config;
    private final ParticleList_1_13 particleList;
    private final ParticleLayout.Settings settings;
    // Each run sends the next turn of the dots of the lines
    private int turn = 0;

    private SendParticles(Plugin plugin, BukkitConfig config, ParticleList_1_13 particleList) {
        this.plugin = plugin;
        this.config = config;
        this.particleList = particleList;
        this.settings = new ParticleLayout.Settings(config.posDensity, config.lineDensity, config.lineMaxLength);
    }

    private void copyAndSend() {
        List<Target> targets = new ArrayList<>();
        for (Map.Entry<UUID, PosData> entry : getPosMap().entrySet()) {
            PosData posData = entry.getValue();
            Player player = getPlayer(entry.getKey());
            if (player == null || !posData.isIn(BukkitAdapter.adapt(player.getWorld()))) continue;

            // Never changed, so it can be read from the asynchronous task
            NavigableMap<Integer, CurvePoint> positions = posData.points();
            if (positions.isEmpty()) continue;

            Location location = player.getLocation();
            Vector3 playerPosition = Vector3.at(location.getX(), location.getY(), location.getZ());
            targets.add(new Target(player, player.getWorld(), positions, playerPosition));
        }

        int turn = this.turn;
        this.turn = (turn + 1) % ParticleLayout.TURNS;
        if (targets.isEmpty()) return;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Target target : targets) {
                send(target, turn);
            }
        });
    }

    private void send(Target target, int turn) {
        List<ParticleLayout.Particle> particles = ParticleLayout.layout(
                target.positions(),
                target.playerPosition(),
                turn,
                settings
        );
        for (ParticleLayout.Particle particle : particles) {
            Location location = getLocation(target.world(), particle.position());
            ParticlePacket packet = switch (particle.kind()) {
                case START -> dust(config.startColor, location);
                case END -> dust(config.endColor, location);
                case POINT -> dust(config.posColor, location);
                case F -> dust(config.fColor, location);
                case B -> dust(config.bColor, location);
                case CENTER, HANDLE_LINE -> particleList.SOUL_FIRE_FLAME.packet(true, location);
                case CURVE -> particleList.FLAME.packet(true, location);
            };
            packet.sendTo(target.player());
        }
    }

    private ParticlePacket dust(Color color, Location location) {
        return particleList.DUST.color(color, 1D).packet(true, location);
    }

    private static Location getLocation(World world, Vector3 position) {
        return new Location(world, position.x(), position.y(), position.z());
    }
}
