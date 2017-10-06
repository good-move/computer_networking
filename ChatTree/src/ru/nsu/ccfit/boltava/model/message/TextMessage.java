package ru.nsu.ccfit.boltava.model.message;

import ru.nsu.ccfit.boltava.model.IMessageHandler;

import javax.xml.bind.annotation.*;
import java.util.UUID;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class TextMessage extends Message {

    @XmlElement(name = "text")
    private final String mText;

    @XmlElement(name = "sender")
    private final String mSenderName;

    public TextMessage() {
        this("","");
    }

    public TextMessage(final String text, final String senderName) {
        super(UUID.randomUUID());
        mText = text;
        mSenderName = senderName;
    }

    @Override
    public void handle(final IMessageHandler handler) {
        handler.handle(this);
    }

    public String getText() {
        return mText;
    }

    public String getSenderName() {
        return mSenderName;
    }

}
