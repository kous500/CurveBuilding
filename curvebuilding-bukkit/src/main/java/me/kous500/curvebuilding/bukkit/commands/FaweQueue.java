package me.kous500.curvebuilding.bukkit.commands;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.session.request.Request;
import me.kous500.curvebuilding.commands.bc.EditRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Runs the edit on a thread of FastAsyncWorldEdit, after the WorldEdit commands that the player ran before, so that
 * the server goes on meanwhile. {@code //cancel} and leaving the server stop it, as they stop those commands.
 */
final class FaweQueue implements EditRunner {
    private final Method queueAction;

    private FaweQueue(Method queueAction) {
        this.queueAction = queueAction;
    }

    /**
     * The runner for the WorldEdit that is installed.
     *
     * @return the queue of FastAsyncWorldEdit, or {@link EditRunner#DIRECT} with a WorldEdit without one
     */
    @SuppressWarnings("JavaReflectionMemberAccess") // Only in FastAsyncWorldEdit, not in the WorldEdit compiled with
    static EditRunner find() {
        try {
            return new FaweQueue(Actor.class.getMethod("queueAction", Runnable.class));
        } catch (NoSuchMethodException e) {
            return DIRECT;
        }
    }

    @Override
    public void run(Player player, Edit edit) {
        Runnable action = () -> {
            // FastAsyncWorldEdit drops its own commands too when the player left before their turn
            if (!player.getSessionKey().isActive()) return;
            try {
                // Where FastAsyncWorldEdit looks for the edits to cancel
                edit.run(editSession -> Request.request().setEditSession(editSession));
            } finally {
                Request.reset();
            }
        };
        try {
            queueAction.invoke(player, action);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not queue //bc in FastAsyncWorldEdit", e);
        }
    }
}
