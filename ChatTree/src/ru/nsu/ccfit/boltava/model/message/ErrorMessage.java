package ru.nsu.ccfit.boltava.model.message;

import ru.nsu.ccfit.boltava.model.IMessageHandler;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.UUID;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class ErrorMessage extends Message {

    @XmlElement
    private long mErrorCode;

    @XmlElement
    private String mErrorMsg;

    public ErrorMessage() {
        super(UUID.randomUUID());
    }

    public String getErrorMsg() {
        return mErrorMsg;
    }

    public long getErrorCode() {
        return mErrorCode;
    }

    @Override
    public void handle(IMessageHandler handler) {
        handler.handle(this);
    }
}
