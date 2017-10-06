package ru.nsu.ccfit.boltava.message;

import ru.nsu.ccfit.boltava.IMessageHandler;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.net.InetSocketAddress;
import java.util.UUID;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class RejoinMessage extends Message {

    private InetSocketAddress mNextParentAddress;

    public RejoinMessage() {
        this(null);
    }

    public RejoinMessage(InetSocketAddress nextParentAddress) {
        super(UUID.randomUUID());
        mNextParentAddress = nextParentAddress;
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handle(this);
    }

    public InetSocketAddress getNextParent() {
        return mNextParentAddress;
    }

}
