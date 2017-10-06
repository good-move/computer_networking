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

        if (!mId.equals(message.mId)) return false;
        return mSender != null ? mSender.equals(message.mSender) : message.mSender == null;
    }

    @Override
    public int hashCode() {
        int result = mId.hashCode();
        result = 31 * result + (mSender != null ? mSender.hashCode() : 0);
        return result;
    }

}
