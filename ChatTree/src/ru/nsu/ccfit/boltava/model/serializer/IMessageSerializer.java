package ru.nsu.ccfit.boltava.model.serializer;

import ru.nsu.ccfit.boltava.model.message.Message;

import javax.xml.bind.JAXBException;

public interface IMessageSerializer<T> {

    T serialize(Message message) throws JAXBException;
    Message deserialize(T type) throws JAXBException;

}
