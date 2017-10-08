package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.message.*;

public interface IMessageHandler {

    /**
     * Acknowledge serializer handling depends on the Client's state nad serializer type
     * it refers to. Incoming ACK serializer must always have a matching pair sent
     * serializer, identified by UUID.
     *
     * - State == STARTING
     * ACK serializer is only expected to confirm a previously sent JOIN serializer.
     * All other messages are ignored.
     *
     * - State == RUNNING
     * Expected ACK messages as response to previously sent:
     *  1. TEXT
     *  2. LEAVE
     *  3. REJOIN
     *  4. ROOT
     * messages.
     *
     * - State == SHUTTING
     *
     * ---- IN DEVELOPMENT ----
     *
     * - State == TERMINATED
     * All messages are ignored and are actually never expected to reach a serializer
     * handler
     *
     * @param message Message that confirms a process has been finished
     *                successfully
     */
    void handle(AckMessage message);

    void handle(ErrorMessage message);

    /**
     * Other node's request to become this node's child and join chat tree.
     * This request is only processed when in RUNNING state.
     * @param message
     */
    void handle(JoinMessage message);

    /**
     * Message to send to the node's parent in the event of shut down. Indicates
     * that the parent should no longer consider this node to be its child.
     *
     * @param message
     */
    void handle(LeaveMessage message);

    /**
     * Signals that the parent wants us to join another node and consider it a parent
     * from now on
     *
     * @param message
     */
    void handle(RejoinMessage message);

    void handle(RootMessage message);

    /**
     * If text serializer is received and current node is not in JOINING or
     * TERMINATED state, then the serializer must be registered by the node, its text
     * should be shown to the user, and ACK response should be sent back to the
     * sender
     *
     * @param message Text serializer which users send via chat
     */
    void handle(TextMessage message);

}
