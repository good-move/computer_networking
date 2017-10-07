package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.event.IEventListener;
import ru.nsu.ccfit.boltava.model.event.MessageReceivedEvent;
import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.net.RobustMessageSender;
import ru.nsu.ccfit.boltava.model.serializer.XmlMessageSerializer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * Neighbor represents a Node's adjacent node.
 * The class provides message sending interface.
 */
public class Neighbor {

    private static final int MAX_THREADS_COUNT = 3;
    private static final int QUEUE_SIZE = 500;

    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS_COUNT);
    private LinkedBlockingQueue<Message> msgDeque = new LinkedBlockingQueue<>(QUEUE_SIZE);

    private final InetSocketAddress address;
    private final DatagramSocket socket;

    public Neighbor(DatagramSocket socket, InetSocketAddress address) throws IOException, JAXBException {
        this.socket = socket;
        this.address = address;

//        MessageSender sender = new MessageSender();
        for (int i = 0; i < MAX_THREADS_COUNT; ++i) {
            threadPool.submit(new MessageSender());
        }
    }

    /**
     * Adds message to the message queue with some priorities:
     *  - ACK message has the highest priority
     *  - Other system messages have the same priority
     *  - Text messages have lowest priority
     *
     *  The only guarantee is that Message types with higher priority will
     *  likely be sent before those having lower priority score.
     *
     * @param message - message to send to the neighbor
     */
    public void feedMessage(Message message) throws InterruptedException {
        msgDeque.put(message);
    }

    public void detach() {
        if (!threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }

    private final class MessageSender implements Runnable, IEventListener<MessageReceivedEvent> {

        private RobustMessageSender sender;

        MessageSender() throws IOException, JAXBException {
            sender = new RobustMessageSender(socket, new XmlMessageSerializer());
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                Message message;
                try {
                    message = msgDeque.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                try {
                    // subscribe to message event
                    sender.send(message, address);
                } catch (JAXBException | IOException | TimeoutException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    // unsubscribe from message event
                }

            }
        }

        @Override
        public void act(MessageReceivedEvent event) {
            sender.cancel();
        }

    }

}
