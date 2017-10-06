package ru.nsu.ccfit.boltava;

import ru.nsu.ccfit.boltava.message.*;

public interface IMessageHandler {

    /**
     * Acknowledge message handling depends on the Node's state nad message type
     * it refers to. Incoming ACK message must always have a matching pair sent
     * message, identified by UUID.
     *
     * - State == STARTING
     * ACK message is only expected to confirm a previously sent JOIN message.
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
     * All messages are ignored and are actually never expected to reach a message
     * handler
     *
     * @param message Message that confirms a process has been finished
     *                successfully
     */
    void handle(AckMessage message);

    void handle(ErrorMessage message);

    /**
     * Other node's request to become this node's child and join chat tree.
     * This request is only processed when in Running state.
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
     * If text message is received and current node is not in Starting or
     * Terminated state, then the message must be registered by the node, its text
     * should be shown to the user, and ACK response should be sent back to the
     * sender
     *
     * @param message Text message which users send via chat
     */
    void handle(TextMessage message);

}
