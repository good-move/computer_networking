package ru.nsu.ccfit.boltava.model;

import ru.nsu.ccfit.boltava.model.event.EventDispatcher;
import ru.nsu.ccfit.boltava.model.event.IEventListener;
import ru.nsu.ccfit.boltava.model.event.AckReceivedEvent;
import ru.nsu.ccfit.boltava.model.message.Message;
import ru.nsu.ccfit.boltava.model.net.RobustMessageSender;
import ru.nsu.ccfit.boltava.model.serializer.XmlMessageSerializer;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * Neighbor represents a Client's adjacent node.
 * The class provides message sending interface.
 */
public class Neighbor {

    private static final int MAX_THREADS_COUNT = 1;
    private static final int QUEUE_SIZE = 500;

    private EventDispatcher<AckReceivedEvent> eventDispatcher;
    private ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS_COUNT);
    private LinkedBlockingQueue<ExecutionPack> queue = new LinkedBlockingQueue<>(QUEUE_SIZE);

    private final InetSocketAddress address;
    private final DatagramSocket socket;

    public Neighbor(DatagramSocket socket, InetSocketAddress address, EventDispatcher<AckReceivedEvent> dispatcher)
            throws IOException, JAXBException {
        this.socket = socket;
        this.address = address;
        this.eventDispatcher = dispatcher;

//        MessageSender sender = new MessageSender();
//        for (int i = 0; i < MAX_THREADS_COUNT; ++i) {
//            threadPool.submit(new MessageSender());
//        }
        new Thread(new MessageSender(), "Neighbor").start();
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
    public CompletableFuture<Message> sendMessage(Message message) throws InterruptedException {
        return feedQueue(message, null, null, null);
    }

    public CompletableFuture<Message> sendMessage(Message message, Runnable onSuccess, Runnable onError) throws InterruptedException {
        return feedQueue(message, onSuccess, onError, null);
    }

    public CompletableFuture<Message> sendMessage(Message message, Runnable onResult) throws InterruptedException {
        return feedQueue(message, null, null, onResult);
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public void detach() {
        if (!threadPool.isShutdown()) {
            threadPool.shutdown();
        }
    }

    private CompletableFuture<Message> feedQueue(Message message,
                                                 Runnable onSuccess,
                                                 Runnable onError,
                                                 Runnable onAny) throws InterruptedException {
        if (message == null) {
            throw new IllegalArgumentException("Message can't be null");
        }
        CompletableFuture<Message> future = new CompletableFuture<>();

        queue.put(new ExecutionPack(message, onSuccess, onError, onAny, future));
        System.out.println("OFFERED MESSAGE");

        return future;
    }

    private final class MessageSender implements Runnable, IEventListener<AckReceivedEvent> {

        private RobustMessageSender sender;

        MessageSender() throws IOException, JAXBException {
            sender = new RobustMessageSender(socket, new XmlMessageSerializer());
        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                ExecutionPack pack;
                try {
                    pack = queue.take();
                    System.out.println("Sending new message to neighbor ");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                AckReceivedEvent event = new AckReceivedEvent(
                        address, pack.getMessage().getId()
                );
                try {
                    System.out.println("subscribing.");
                    eventDispatcher.subscribe(event, this);
                    sender.send(pack.getMessage(), address);
                } catch (JAXBException | IOException | InterruptedException e) {
                    e.printStackTrace();
                    if (pack.getOnError() != null) {
                        pack.getOnError().run();
                    }
                } catch (RobustMessageSender.SenderStoppedException e) {
                    System.out.println("trying to run onSuccess");
                    if (pack.getOnSuccess() != null) {
                        pack.getOnSuccess().run();
                    }
                } catch (TimeoutException e) {
                    System.out.println("trying to run onError");
                    System.out.println(e.getMessage());
                    if (pack.getOnError() != null) {
                        pack.getOnError().run();
                    }
                    pack.getFuture().cancel(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println(e.getMessage());
                }
                finally {
                    if (pack.onError == null &&
                        pack.onSuccess == null &&
                        pack.onResult != null) {
                        pack.onResult.run();
                    }
                    System.out.println("unsubscribing.");
                    pack.getFuture().complete(pack.getMessage());
                    eventDispatcher.unsubscribe(event, this);
                }

            }
        }

        @Override
        public void act(AckReceivedEvent event) {
            System.out.println("Received ACK from " + address);
            sender.cancel();
        }

    }

    private final class ExecutionPack {

        private final Message message;
        private final Runnable onSuccess;
        private final Runnable onError;
        private final Runnable onResult;
        private final CompletableFuture<Message> future;

        ExecutionPack(Message message,
                      Runnable onSuccess,
                      Runnable onError,
                      Runnable onResult, CompletableFuture<Message> future) {
            this.message = message;
            this.onSuccess = onSuccess;
            this.onError = onError;
            this.onResult = onResult;
            this.future = future;
        }

        public Message getMessage() {
            return message;
        }

        public Runnable getOnSuccess() {
            return onSuccess;
        }

        public Runnable getOnError() {
            return onError;
        }

        public CompletableFuture<Message> getFuture() {
            return future;
        }
    }

}
