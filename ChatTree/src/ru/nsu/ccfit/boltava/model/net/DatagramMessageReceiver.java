package ru.nsu.ccfit.boltava.model.net;

import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.serializer.IMessageSerializer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.Charset;


/**
 * Datagram Message receiver accepts DatagramSocket and handles incoming packages,
 * retrieving Message objects from them.
 */
public class DatagramMessageReceiver {

    private static final int MAX_BUFFER_SIZE = 1000;

    private final DatagramSocket socket;
    private final IMessageSerializer<String> serializer;

    public DatagramMessageReceiver(DatagramSocket socket, IMessageSerializer<String> serializer) {
        this.socket = socket;
        this.serializer = serializer;
    }

    /**
     * Blocks until a packet of at most `bufferSize` bytes is received.
     *
     * @param bufferSize - max packet size in bytes
     * @return
     * @throws IOException
     * @throws JAXBException
     * @throws IllegalArgumentException
     */
    public MessageWrapper receive(int bufferSize) throws IOException, JAXBException, IllegalArgumentException {
        if (bufferSize < 0 || bufferSize > MAX_BUFFER_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Buffer size must be an integer in range 0...%d", MAX_BUFFER_SIZE)
            );
        }

        return receivePacket(bufferSize);
    }

    /**
     * Blocks until a packet of at most MAX_BUFFER_SIZE bytes is received.
     * @return
     * @throws IOException
     * @throws JAXBException
     */
    public MessageWrapper receive() throws IOException, JAXBException {
        return receivePacket(MAX_BUFFER_SIZE);
    }

    private MessageWrapper receivePacket(int bufferSize) throws IOException, JAXBException {
        byte[] bytes = new byte[bufferSize];
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        socket.receive(packet);
        String stringMessage = new String(packet.getData(), 0, packet.getLength(), Charset.forName("utf-8"));
        Message message = serializer.deserialize(stringMessage);
        return new MessageWrapper(message, packet);
    }

    public static final class MessageWrapper {

        private final Message message;
        private final DatagramPacket packet;

        MessageWrapper(Message message, DatagramPacket packet) {
            this.message = message;
            this.packet = packet;
        }


        public Message getMessage() {
            return message;
        }

        public DatagramPacket getPacket() {
            return packet;
        }

        public int getDataLength() {
            return packet.getLength();
        }

        public InetAddress getAddress() {
            return packet.getAddress();
        }

        public int getPort() {
            return packet.getPort();
        }

    }

}
