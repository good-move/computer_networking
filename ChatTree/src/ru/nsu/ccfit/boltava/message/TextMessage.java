package ru.nsu.ccfit.boltava.message;

import ru.nsu.ccfit.boltava.IMessageHandler;

import javax.xml.bind.annotation.*;
import java.util.UUID;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TextMessage extends Message {

    private String mText;

    private String mSenderName;

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
