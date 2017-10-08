package ru.nsu.ccfit.boltava.model.message;

import ru.nsu.ccfit.boltava.model.IMessageHandler;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.net.InetSocketAddress;
import java.util.UUID;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class Message {

    @XmlElement(name = "uuid")
    private final UUID mId;

    @XmlTransient
    private InetSocketAddress mSender;

    Message(UUID uuid) {
        mId = uuid;
    }

    public UUID getId() {
        return mId;
    }

    public abstract void handle(IMessageHandler handler);

    public InetSocketAddress getSender() {
        return mSender;
    }

    public void setSender(InetSocketAddress mSender) {
        this.mSender = mSender;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        return mId.equals(message.mId);
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }
}
