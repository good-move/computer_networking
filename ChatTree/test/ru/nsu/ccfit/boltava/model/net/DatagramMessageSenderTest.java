package ru.nsu.ccfit.boltava.model.net;

import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramSocket;

import static org.junit.jupiter.api.Assertions.*;


public class DatagramMessageSenderTest {

    @Test
    public void shouldThrowOnNullSocket() {
        assertThrows(IllegalArgumentException.class, this::createNullSocketSender);
    }

    @Test
    public void shouldThrowOnNullSerializer() {
        assertThrows(IllegalArgumentException.class, this::createNullSerializerSender);
    }

    private void createNullSocketSender() throws IOException, JAXBException {
        new DatagramMessageSender(null, null);
    }

    private void createNullSerializerSender() throws IOException, JAXBException {
        new DatagramMessageSender(new DatagramSocket(4444), null);
    }

}
