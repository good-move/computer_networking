package ru.nsu.ccfit.boltava.model.net;

import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.serializer.IMessageSerializer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;


/**
 * RobustMessageSender is supposed to make delivery of Datagram packets more
 * robust, by sending them repeatedly until an acknowledge message is received.
 */
public class RobustMessageSender {

    private final static int DEFAULT_TIMEOUT = 10 * 1000;
    private final static int WAIT_INTERVAL = 250;
    private final DatagramMessageSender sender;

    public RobustMessageSender(DatagramSocket socket, IMessageSerializer<String> serializer)
            throws IOException, JAXBException {
        sender = new DatagramMessageSender(socket, serializer);
    }

    /**
     *
     * @param message - message to send
     * @param receiver - address of message receiver
     * @throws JAXBException
     * @throws IOException
     * @throws TimeoutException
     */
    public void send(Message message, InetSocketAddress receiver)
            throws JAXBException, IOException, TimeoutException, InterruptedException {
        this.send(message, receiver, DEFAULT_TIMEOUT);
    }


    /**
     * Repeatedly sends packet, until either the sender is stopped or the timeout
     * hits.
     *
     * @param message - message to send
     * @param receiver - address of message receiver
     * @param timeout - maximum time in milliseconds, during which attempts to
     *                deliver the message will be made
     * @throws JAXBException
     * @throws IOException
     * @throws TimeoutException
     */
    public void send(Message message, InetSocketAddress receiver, int timeout)
            throws JAXBException, IOException, TimeoutException, InterruptedException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            sender.send(message, receiver);
            wait(WAIT_INTERVAL);
        }
        throw new TimeoutException("Delivery timeout hit");
    }

}
