package me.kous500.curvebuilding.config;

/**
 * The messages in the message files ({@code messages/<language>.yml}). {@code {0}}, {@code {1}}, ... in a message are
 * replaced with the arguments given with it, in that order.
 */
public enum Message {
    NON_PLAYER_EXECUTION("non-player-execution"),
    INCORRECT_ARGUMENT("incorrect-argument"),
    POS_NOT_EXIST("pos-not-exist"),
    POS_N("pos-N"),
    POS_NF("pos-Nf"),
    POS_NB("pos-Nb"),
    POS_SET("pos-set"),
    POS_CLEAR("pos-clear"),
    POS_CLEAR_ALL("pos-clear-all"),
    POS_REMOVE("pos-remove"),
    POS_SHIFT("pos-shift"),
    BC_CHANGED("bc-changed"),
    BC_LENGTH("bc-length"),
    INCOMPLETE_REGION("incomplete-region"),
    INCOMPLETE_POS("incomplete-pos"),
    MAX_CHANGED_BLOCKS("max-changed-blocks"),
    MAX_SET_LENGTH("max-set-length"),
    MAX_POS_VALUE("max-pos-value");

    /** The path of the message in the message files. */
    public final String path;

    Message(String key) {
        this.path = "messages." + key;
    }
}
