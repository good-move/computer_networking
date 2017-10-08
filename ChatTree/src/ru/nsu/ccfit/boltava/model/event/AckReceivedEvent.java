package ru.nsu.ccfit.boltava.model.event;

import com.sun.istack.internal.NotNull;

import java.net.SocketAddress;
import java.util.UUID;

public class MessageReceivedEvent extends Event {

    private final String HASHCODE_SEED = MessageReceivedEvent.class.getCanonicalName();
    private final SocketAddress sender;
    private final UUID messageId;

    public MessageReceivedEvent(@NotNull final SocketAddress sender, @NotNull final UUID messageId) {
        this.sender = sender;
        this.messageId = messageId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public SocketAddress getSender() {
        return sender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessageReceivedEvent that = (MessageReceivedEvent) o;

        if (!HASHCODE_SEED.equals(that.HASHCODE_SEED)) return false;
        if (!sender.equals(that.sender)) return false;
        return messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        int result = HASHCODE_SEED.hashCode();
        result = 31 * result + sender.hashCode();
        result = 31 * result + messageId.hashCode();
        return result;
    }

}
