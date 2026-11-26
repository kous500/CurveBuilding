package me.kous500.curvebuilding.commands.bc;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Player;

import java.util.function.Consumer;

/** Where the edit of {@code //bc} is run: during the command, or later on a thread that WorldEdit can edit on. */
@FunctionalInterface
public interface EditRunner {
    /** Runs the edit on the thread of the command, before the command ends. */
    EditRunner DIRECT = (player, edit) -> edit.run(editSession -> { });

    /**
     * Runs {@code edit} once, or has it run once later.
     *
     * @param player the player who ran the command
     * @param edit the edit to run
     */
    void run(Player player, Edit edit);

    /** An edit to run. */
    @FunctionalInterface
    interface Edit {
        /**
         * Edits the world until it is done.
         *
         * @param started given the edit session before anything is read or placed with it
         */
        void run(Consumer<EditSession> started);
    }
}
