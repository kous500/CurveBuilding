package me.kous500.curvebuilding.bukkit;

import me.kous500.curvebuilding.storage.PlayerStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Reads the curve of a player from their file when they join, and removes it when they leave. */
public final class PlayerFiles implements Listener {
    private final Supplier<@Nullable PlayerStore> store;

    /**
     * Reads and removes the curves with the store that {@code store} gives at the time.
     *
     * @param store gives the store of the files of the players, or null while there is none
     */
    public PlayerFiles(Supplier<@Nullable PlayerStore> store) {
        this.store = store;
    }

    /**
     * Reads the curve of the player who joined, before the other plugins see them join.
     *
     * @param event the join
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        PlayerStore current = store.get();
        if (current != null) current.load(event.getPlayer().getUniqueId());
    }

    /**
     * Removes the curve of the player who left, after the other plugins see them leave.
     *
     * @param event the leave
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        PlayerStore current = store.get();
        if (current != null) current.unload(event.getPlayer().getUniqueId());
    }
}
