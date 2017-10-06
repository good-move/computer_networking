package ru.nsu.ccfit.boltava.message;

import ru.nsu.ccfit.boltava.IMessageHandler;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class JoinMessage extends Message {

    @XmlElement(name = "name")
    private final String mNodeName;

    public JoinMessage() {
        this("nobody");
    }

    public JoinMessage(String nodeName) {
        super(UUID.randomUUID());
        mNodeName = nodeName;
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handle(this);
    }

    public String getNodeName() {
        return mNodeName;
    }
}
