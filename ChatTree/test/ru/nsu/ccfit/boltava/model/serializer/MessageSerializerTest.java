package ru.nsu.ccfit.boltava.model.serializer;

import org.junit.Before;
import org.junit.Test;
import ru.nsu.ccfit.boltava.model.message.*;

import javax.xml.bind.JAXBException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

public class MessageSerializerTest {

    private static XmlMessageSerializer serializer = null;

    @Before
    public void initSerializer() throws JAXBException {
        try {
            serializer = new XmlMessageSerializer();
        } catch (JAXBException e) {
            e.printStackTrace();
            System.err.println("Failed to initialize xml message serializer");
            throw e;
        }
    }

    @Test
    public void checkJoinMessageSerialization() {
        try {
            JoinMessage inputMessage = new JoinMessage("Alex");
            final String xml = serializer.serialize(inputMessage);
            JoinMessage outputMessage = (JoinMessage) serializer.deserialize(xml);
            assertEquals(true, inputMessage.getNodeName().equals(outputMessage.getNodeName()));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkRejoinMessageSerialization() {
        try {
            RejoinMessage inputMessage = new RejoinMessage(new InetSocketAddress(InetAddress.getByName("localhost"),1));
            final String xml = serializer.serialize(inputMessage);
            RejoinMessage outputMessage = (RejoinMessage) serializer.deserialize(xml);
            assertTrue(outputMessage.getAddress().equals(inputMessage.getAddress()));
            assertTrue(outputMessage.getPort() == inputMessage.getPort());
        } catch (JAXBException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkTextMessageSerialization() {
        try {
            TextMessage inputMessage = new TextMessage("Message", "Sender Name");
            final String xml = serializer.serialize(inputMessage);
            TextMessage outputMessage = (TextMessage) serializer.deserialize(xml);
            assertTrue(outputMessage.getSenderName().equals(inputMessage.getSenderName()));
            assertEquals(outputMessage.getText(), inputMessage.getText());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void checkDumbMessageSerialization() {
        Class types[] = { AckMessage.class, LeaveMessage.class, RootMessage.class, ErrorMessage.class };
        for (Class c : types) {
            try {
                checkMessageOfType((Message)c.newInstance(), c);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private <T extends Message> void checkMessageOfType(T inputMessage, Class<T> messageClass) {
        try {
            final String xml = serializer.serialize(inputMessage);
            T outputMessage = (messageClass.cast(serializer.deserialize(xml)));
            assertEquals(true, inputMessage.getId().equals(outputMessage.getId()));
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

}
