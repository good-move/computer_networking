package ru.nsu.ccfit.boltava.serializer;

import ru.nsu.ccfit.boltava.message.Message;

import javax.xml.bind.JAXBException;

public interface IMessageSerializer<T> {

    T serialize(Message message) throws JAXBException;
    Message deserialize(T type) throws JAXBException;

}
