package ru.nsu.ccfit.boltava.view;

import ru.nsu.ccfit.boltava.model.message.TextMessage;

public interface IMessageListener {

    void onTextMessageEntered(String message);

}
