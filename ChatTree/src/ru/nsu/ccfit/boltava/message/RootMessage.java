package ru.nsu.ccfit.boltava.message;

import ru.nsu.ccfit.boltava.IMessageHandler;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class RootMessage extends Message {

    public RootMessage() {
        super(UUID.randomUUID());
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handle(this);
    }

}
