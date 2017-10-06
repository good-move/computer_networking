package ru.nsu.ccfit.boltava.model.message;

import ru.nsu.ccfit.boltava.model.IMessageHandler;
import ru.nsu.ccfit.boltava.model.serializer.InetAddressAdapter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class RejoinMessage extends Message {

    @XmlElement(name = "parent_address")
    @XmlJavaTypeAdapter(InetAddressAdapter.class)
    private InetAddress address;

    @XmlElement(name = "parent_port")
    private int port;

    public RejoinMessage() {
        this(null, 0);
    }

    public RejoinMessage(InetSocketAddress address) {
        super(UUID.randomUUID());
        this.address = address.getAddress();
        this.port = address.getPort();
    }

    public RejoinMessage(InetAddress address, int port) {
        super(UUID.randomUUID());
        this.address = address;
        this.port = port;
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handle(this);
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
