package ru.nsu.ccfit.boltava.message;

import ru.nsu.ccfit.boltava.IMessageHandler;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;

@XmlRootElement
public class AckMessage extends Message {

    public AckMessage() {
        this(UUID.randomUUID());
    }

    public AckMessage(UUID messageId) {
        super(messageId);
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handle(this);
    }

}
